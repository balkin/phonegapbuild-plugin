package ru.balkin.jenkins.phonegapbuild;

import hudson.model.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * <p>Description: </p>
 * Date: 6/1/15 - 4:07 PM
 *
 * @author Ruslan Balkin <a href="mailto:baron@baron.su">baron@baron.su</a>
 * @version 1.0.0.0
 */
public class PhonegapBuildBuilderTest {

	public static final String APPLICATION_ID = "123";
	public static final String APPLICATION_TOKEN = "12345_7890abcdefghij";
	public static final String APPLICATION_TITLE = "TestApp";

	PhonegapBuildBuilder phonegapBuilder;

	@Before
	public void beforeMethod() {
		phonegapBuilder = new PhonegapBuildBuilder(APPLICATION_ID, APPLICATION_TOKEN, APPLICATION_TITLE, true);
	}

	@Test
	public void testPerform() throws Exception {
		final FakeItemGroup itemGroup = new FakeItemGroup(new File("/tmp/fakeProject"));
		final FreeStyleProject p = new FreeStyleProject(itemGroup, "freestyle") {
			/**
			 * Allocates a new buildCommand number.
			 */
			@Override
			public synchronized int assignBuildNumber() throws IOException {
				return 1;
			}

			/**
			 * Directory for storing {@link Run} records.
			 * <p/>
			 * Some {@link Job}s may not have backing data store for {@link Run}s, but
			 * those {@link Job}s that use file system for storing data should use this
			 * directory for consistency.
			 *
			 * @see RunMap
			 */
			@Override
			public File getBuildDir() {
				return itemGroup.getRootDir();
			}
		};
		final AbstractBuild build = new FreeStyleBuild(p);

		phonegapBuilder.perform(build, null, new FakeBuildListener());
	}

	@Test
	public void testGetApplicationId() throws Exception {
		Assert.assertEquals("Application Id should match", APPLICATION_ID, phonegapBuilder.getApplicationId());
	}

	@Test
	public void testGetAuthToken() throws Exception {
		Assert.assertEquals("AuthToken should match", APPLICATION_TOKEN, phonegapBuilder.getAuthToken());
	}

	@Test
	public void testGetApplicationTitle() throws Exception {
		Assert.assertEquals("App title should match", APPLICATION_TITLE, phonegapBuilder.getApplicationTitle());
	}

	@Test
	public void testIsApplicationShare() throws Exception {

	}
}