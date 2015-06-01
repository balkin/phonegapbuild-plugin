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
import org.apache.commons.httpclient.methods.multipart.*;
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
	private final Boolean applicationShare;

	// Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
	@DataBoundConstructor
	public PhonegapBuildBuilder(String applicationId, String authToken, String applicationTitle, Boolean applicationShare) {
		this.applicationId = applicationId;
		this.authToken = authToken;
		this.applicationTitle = applicationTitle;
		this.applicationShare = applicationShare;
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

	public Boolean isApplicationShare() {
		return applicationShare;
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
			final File wwwDir = new File(build.getRootDir(), "www");
			logger.printf("Zipping %s%n", wwwDir.getAbsolutePath());
			addToZip(zos, wwwDir, null);
			final File resourcesDir = new File(build.getRootDir(), "resources");
			logger.printf("Zipping %s%n", resourcesDir.getAbsolutePath());
			addToZip(zos, resourcesDir, null);
			final File configXmlFile = new File(build.getRootDir(), "config.xml");
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

		final HttpClientParams params = new HttpClientParams();
		params.setConnectionManagerTimeout(60000);
		final HttpClient client = new HttpClient(params);
		final String urlString = "https://build.phonegap.com/api/v1/apps/" + getApplicationId();

		final Map<String, Object> jsonMap = new HashMap<String, Object>();
		jsonMap.put("create_method", "file");
		jsonMap.put("title", getApplicationTitle());
		jsonMap.put("share", isApplicationShare());
		final JsonBuilder jsonBuilder = new JsonBuilder(jsonMap);

		PostMethod post = new PostMethod(urlString);

		try {
			final Part authTokenPart = new StringPart("auth_token", getAuthToken());
			final Part dataPart = new StringPart("data", jsonBuilder.toString());
			final Part filePart = new FilePart("phonegap.zip", new ByteArrayPartSource("phonegap.zip", bos.toByteArray()));
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
}
