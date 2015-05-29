package ru.balkin.jenkins.phonegapbuild;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Phonegap Build support for Jenkins
 * <p></p>
 *
 * @author Ruslan Balkin
 */
public class PhonegapBuildBuilder extends Builder {

	private final String applicationId;

	// Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
	@DataBoundConstructor
	public PhonegapBuildBuilder(String applicationId) {
		this.applicationId = applicationId;
	}

	/**
	 * We'll use this from the <tt>config.jelly</tt>.
	 */
	public String getApplicationId() {
		return applicationId;
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
		final String authenticationToken = getDescriptor().getAuthenticationToken();
		listener.getLogger().println("Building using authentication token " + authenticationToken);
		return true;
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
		private String authenticationToken;

		/**
		 * In order to load the persisted global configuration, you have to
		 * call load() in the constructor.
		 */
		public DescriptorImpl() {
			load();
		}

		/**
		 * Performs on-the-fly validation of the form field 'name'.
		 *
		 * @param value This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the browser.
		 * <p/>
		 * Note that returning {@link FormValidation#error(String)} does not
		 * prevent the form from being saved. It just means that a message
		 * will be displayed to the user.
		 */
		public FormValidation doCheckName(@QueryParameter String value)
				throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please set a name");
			if (value.length() < 4)
				return FormValidation.warning("Isn't the name too short?");
			return FormValidation.ok();
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
			authenticationToken = formData.getString("authenticationToken");
			save();
			return super.configure(req, formData);
		}

		public String getAuthenticationToken() {
			return authenticationToken;
		}
	}
}

