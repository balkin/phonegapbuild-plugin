package ru.balkin.jenkins.phonegapbuild;

import groovy.json.JsonBuilder;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.tools.zip.ZipEntry;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.*;
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
			logger.printf("Zipping %s%n", new File("www").getAbsolutePath());
			addToZip(zos, new File("www"), null);
			addToZip(zos, new File("resources"), null);
			addToZip(zos, new File("config.xml"), null);
		} catch (Exception e) {
			logger.printf("Failed to create zip file: %s%n", e.getMessage());
			e.printStackTrace(logger);
			return false;
		}

		final HttpClientParams params = new HttpClientParams();
		params.setConnectionManagerTimeout(60000);
		final HttpClient client = new HttpClient(params);
		final String urlString = "https://build.phonegap.com/api/v1/apps/" + getApplicationId();

		final Map<String, Object> jsonMap = new HashMap<String, Object>();
		jsonMap.put("create_method", "file");
		jsonMap.put("title", getApplicationTitle());
		jsonMap.put("share", true);
		final JsonBuilder jsonBuilder = new JsonBuilder(jsonMap);

		PostMethod post = new PostMethod(urlString);

		try {
			final Part authTokenPart = new StringPart("auth_token", getAuthToken());
			final Part dataPart = new StringPart("data", jsonBuilder.toString());
			final Part filePart = new FilePart("file", new File("/tmp/phonegap.zip"));
			final HttpMethodParams postParams = new HttpMethodParams();
			post.setRequestEntity(new MultipartRequestEntity(new Part[]{authTokenPart, dataPart, filePart}, postParams));
			int res = client.executeMethod(post);
			logger.printf("Executed POST method, response is %d%n", res);
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace(logger);
		} catch (HttpException e) {
			e.printStackTrace(logger);
		} catch (IOException e) {
			e.printStackTrace(logger);
		}
		return false;
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
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			save();
			return super.configure(req, formData);
		}
	}
}
