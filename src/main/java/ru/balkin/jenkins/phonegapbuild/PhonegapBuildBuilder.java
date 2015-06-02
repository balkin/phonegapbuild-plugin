package ru.balkin.jenkins.phonegapbuild;

import com.google.gson.Gson;
import groovy.json.JsonBuilder;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.multipart.*;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.tools.zip.ZipEntry;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipOutputStream;

/**
 * Phonegap Build support for Jenkins
 * <p></p>
 *
 * @author Ruslan Balkin
 */
public class PhonegapBuildBuilder extends Builder {

	public static final String PENDING = "pending";
	public static final String ANDROID = "android";
	public static final String IOS = "ios";
	public static final String WINPHONE = "winphone";
	public static final String COMPLETE = "complete";

	public static final int HTTP_OK = 200;
	public static final int RETRIES = 30;
	public static final int RETRY_DELAY = 10000;
	private final String applicationId;
	private final String authToken;
	private final String applicationTitle;

	// Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
	@DataBoundConstructor
	public PhonegapBuildBuilder(String applicationId, String authToken, String applicationTitle) {
		this.applicationId = applicationId;
		this.authToken = authToken;
		this.applicationTitle = applicationTitle;
	}

	/**
	 * We'll use this from the <tt>config.jelly</tt>.
	 */
	public String getApplicationId() {
		return applicationId;
	}

	public String getAuthToken() {
		return authToken;
	}

	public String getApplicationTitle() {
		return applicationTitle;
	}

	public static void addToZip(ZipOutputStream zos, File fileToZip, String parentDirectory) throws Exception {
		if (fileToZip == null || !fileToZip.exists()) {
			return;
		}

		String zipEntryName = fileToZip.getName();
		if (parentDirectory != null && !parentDirectory.isEmpty()) {
			zipEntryName = parentDirectory + "/" + fileToZip.getName();
		}

		if (fileToZip.isDirectory()) {
			final File[] files = fileToZip.listFiles();
			if (files != null) {
				for (File file : files) {
					addToZip(zos, file, zipEntryName);
				}
			}
		} else {
			byte[] buffer = new byte[4096];
			FileInputStream fis = new FileInputStream(fileToZip);
			zos.putNextEntry(new ZipEntry(zipEntryName));
			int length;
			while ((length = fis.read(buffer)) > 0) {
				zos.write(buffer, 0, length);
			}
			zos.closeEntry();
			fis.close();
		}
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
		final PrintStream logger = listener.getLogger();
		logger.printf("Building application %s (%s) using authentication token %s%n", getApplicationTitle(), getApplicationId(), getAuthToken());

		// Prepare zip
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ZipOutputStream zos = new ZipOutputStream(bos);
		// config.xml www resources
		try {
			final File rootDir = new File(build.getWorkspace().getRemote());
			final File wwwDir = new File(rootDir, "www");
			logger.printf("Zipping %s%n", wwwDir.getAbsolutePath());
			addToZip(zos, wwwDir, null);
			final File resourcesDir = new File(rootDir, "resources");
			logger.printf("Zipping %s%n", resourcesDir.getAbsolutePath());
			addToZip(zos, resourcesDir, null);
			final File configXmlFile = new File(rootDir, "config.xml");
			logger.printf("Zipping %s%n", configXmlFile.getAbsolutePath());
			addToZip(zos, configXmlFile, null);
			zos.flush();
			bos.flush();
			zos.close();
			bos.close();
		} catch (Exception e) {
			logger.printf("Failed to create zip file: %s%n", e.getMessage());
			e.printStackTrace(logger);
			return false;
		}

		final byte[] bytes = bos.toByteArray();
		logger.printf("ZIP file created: %d%n", bytes.length);

		try {
			final FileOutputStream fileOutputStream = new FileOutputStream("/tmp/phonegap.zip");
			fileOutputStream.write(bytes);
			fileOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		final HttpClientParams params = new HttpClientParams();
		params.setConnectionManagerTimeout(60000);
		final HttpClient client = new HttpClient(params);

		final String urlString = "https://build.phonegap.com/api/v1/apps/" + getApplicationId();

		final Map<String, Object> jsonMap = new HashMap<String, Object>();
		jsonMap.put("create_method", "file");
		jsonMap.put("title", getApplicationTitle());
		jsonMap.put("share", true);
		final String jsonData = new JsonBuilder(jsonMap).toString();
		logger.printf("JSON data: %s%n", jsonData);

		PutMethod post = new PutMethod(urlString);

		try {
			final Part authTokenPart = new StringPart("auth_token", getAuthToken());
			final Part dataPart = new StringPart("data", jsonData);
			final Part filePart = new FilePart("file", new ByteArrayPartSource("phonegap.zip", bytes), "application/octet-stream", "utf-8");

			final MultipartRequestEntity multipart = new MultipartRequestEntity(new Part[]{authTokenPart, dataPart, filePart}, post.getParams());
			post.setRequestEntity(multipart);
			int res = client.executeMethod(post);
			if (res != HTTP_OK) {
				logger.printf("Failure! Executed PUT method, response is %d%n", res);
				return false;
			}
			logger.printf("Executed PUT method, response is %d%n", res);
			Boolean x = extractBuilds(build, logger, client, urlString);
			if (x != null) return x;
			logger.println("Phonegap Build failed to create new builds in 300 seconds.");
			return false;
		} catch (FileNotFoundException e) {
			e.printStackTrace(logger);
		} catch (HttpException e) {
			e.printStackTrace(logger);
		} catch (IOException e) {
			e.printStackTrace(logger);
		} catch (InterruptedException e) {
			e.printStackTrace(logger);
		}
		return false;
	}

	private Boolean extractBuilds(AbstractBuild build, PrintStream logger, HttpClient client, String urlString) throws InterruptedException, IOException {
		final Map<String, File> builds = new HashMap<String, File>();
		builds.put(ANDROID, null);
		builds.put(IOS, null);
		builds.put(WINPHONE, null);
		for (int w = 0; w < RETRIES; w++) {
			Thread.sleep(RETRY_DELAY);
			GetMethod get2 = new GetMethod(urlString + "?auth_token=" + getAuthToken());
			int res2 = client.executeMethod(get2);
			if (res2 != HTTP_OK) {
				logger.println("#2 Cannot obtain build information");
				return false;
			}
			final InputStreamReader isr = new InputStreamReader(get2.getResponseBodyAsStream());
			final Map json2 = new Gson().fromJson(isr, Map.class);
			final Object status = json2.get("status");
			final Map download = (Map) json2.get("download");
			if (status instanceof Map) {
				final Map statusMap = (Map) status;
				boolean pending = false;
				for (Map.Entry<String, File> entry : builds.entrySet()) {
					if (entry.getValue() == null) {
						final String platform = entry.getKey();
						final Object buildStatus = statusMap.get(platform);
						if (COMPLETE.equals(buildStatus)) {
							final String tmpUrl = (String) download.get(platform);
							try {
								logger.printf("Downloading %s build from %s%n", platform, tmpUrl);
								final File file = downloadUrl(tmpUrl, build.getRootDir(), logger);
								builds.put(platform, file);
								logger.printf("Downloaded %s build to %s%n", platform, file.getAbsolutePath());
							} catch (IOException ex) {
								logger.printf("Exception while downloading %s build from %s: %s%n", platform, tmpUrl, ex.getMessage());
							}
						} else if (PENDING.equals(buildStatus)) {
							pending = true;
						}
					}
				}
				final Object android = statusMap.get(ANDROID), winphone = statusMap.get(WINPHONE), ios = statusMap.get(IOS);
				logger.printf("Pending: %s. Build status: [ Android: %s | WinPhone: %s | iOS: %s ]%n", pending, android, winphone, ios);
				if (!pending) {
					return true;
				}
			} else {
				logger.printf("Returned status in JSON is not Map, it's %s:%n%s%n", status.getClass().getCanonicalName(), json2);
			}
		}
		return null;
	}

	private File downloadUrl(String tmpUrl, File directory, PrintStream logger) throws IOException {
		final URL phonegap = new URL("https://build.phonegap.com");
		final URLConnection jsonConnection = new URL(phonegap, tmpUrl + "?auth_token=" + getAuthToken()).openConnection();
		final Map json = new Gson().fromJson(new InputStreamReader(jsonConnection.getInputStream()), Map.class);
		final URL binUrl = new URL((String) json.get("location"));
		final URLConnection binConnection = binUrl.openConnection();
		final InputStream input = binConnection.getInputStream();
		byte[] buffer = new byte[4096];
		int n;
		final String binUrlPath = binUrl.getPath();
		final File archiveDir = new File(directory, "archive");
		if (!archiveDir.exists()) archiveDir.mkdir();
		final File file = new File(archiveDir, binUrlPath.substring(binUrlPath.lastIndexOf('/') + 1));
		logger.printf("Downloading %s build to %s%n", binUrl, file);
		OutputStream output = new FileOutputStream(file);
		while ((n = input.read(buffer)) != -1) {
			output.write(buffer, 0, n);
		}
		output.close();
		return file;
	}

	// Overridden for better type safety.
	// If your plugin doesn't really define any property on Descriptor,
	// you don't have to do this.
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	/**
	 * Descriptor for {@link PhonegapBuildBuilder}. Used as a singleton.
	 * The class is marked as public so that it can be accessed from views.
	 * <p/>
	 * <p/>
	 * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
	 * for the actual HTML fragment for the configuration screen.
	 */
	@Extension // This indicates to Jenkins that this is an implementation of an extension point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		/**
		 * In order to load the persisted global configuration, you have to
		 * call load() in the constructor.
		 */
		public DescriptorImpl() {
			load();
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project types
			return true;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "Build mobile apps using Phonegap Build";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
			save();
			return super.configure(req, formData);
		}

	}

}