package ru.balkin.jenkins.phonegapbuild;

import hudson.model.Item;
import hudson.model.ItemGroup;
import org.acegisecurity.AccessDeniedException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * <p>Description: </p>
 * Date: 6/1/15 - 5:29 PM
 *
 * @author Ruslan Balkin <a href="mailto:baron@baron.su">baron@baron.su</a>
 * @version 1.0.0.0
 */
public class FakeItemGroup implements ItemGroup {

	private final File rootDir;

	public FakeItemGroup(File rootDir) {
		this.rootDir = rootDir;
	}

	@Override
	public String getFullName() {
		return null;
	}

	@Override
	public String getFullDisplayName() {
		return null;
	}

	@Override
	public Collection getItems() {
		return null;
	}

	@Override
	public String getUrl() {
		return null;
	}

	@Override
	public String getUrlChildPrefix() {
		return null;
	}

	@Override
	public Item getItem(String name) throws AccessDeniedException {
		return null;
	}

	@Override
	public File getRootDirFor(Item child) {
		return rootDir;
	}

	@Override
	public void onRenamed(Item item, String oldName, String newName) throws IOException {

	}

	@Override
	public void onDeleted(Item item) throws IOException {

	}

	@Override
	public String getDisplayName() {
		return null;
	}

	@Override
	public File getRootDir() {
		return rootDir;
	}

	@Override
	public void save() throws IOException {

	}
}
