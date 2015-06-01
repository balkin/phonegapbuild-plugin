package ru.balkin.jenkins.phonegapbuild;

import org.junit.Assert;
import org.junit.Test;

/**
 * <p>Description: </p>
 * Date: 6/1/15 - 4:07 PM
 *
 * @author Ruslan Balkin <a href="mailto:baron@baron.su">baron@baron.su</a>
 * @version 1.0.0.0
 */
public class PhonegapBuildBuilderTest {

	@Test
	public void testAddToZip() throws Exception {
	}

	@Test
	public void testPerform() throws Exception {

	}

	@Test
	public void testGetApplicationId() throws Exception {
		final PhonegapBuildBuilder pbb = new PhonegapBuildBuilder("123", "authToken", "AppTitle", true);
		Assert.assertEquals("Application Id should be 123", "123", pbb.getApplicationId());
	}

	@Test
	public void testGetAuthToken() throws Exception {
		final PhonegapBuildBuilder pbb = new PhonegapBuildBuilder("123", "authToken", "AppTitle", true);
		Assert.assertEquals("AuthToken should be authToken", "authToken", pbb.getAuthToken());
	}

	@Test
	public void testGetApplicationTitle() throws Exception {
		final PhonegapBuildBuilder pbb = new PhonegapBuildBuilder("123", "authToken", "AppTitle", true);
		Assert.assertEquals("App title should be AppTitle", "AppTitle", pbb.getApplicationTitle());
	}

	@Test
	public void testIsApplicationShare() throws Exception {

	}
}