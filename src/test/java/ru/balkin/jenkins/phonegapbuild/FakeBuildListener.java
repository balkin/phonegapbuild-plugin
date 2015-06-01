package ru.balkin.jenkins.phonegapbuild;

import hudson.console.ConsoleNote;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Result;
import hudson.model.Run;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * <p>Description: </p>
 * Date: 6/1/15 - 5:28 PM
 *
 * @author Ruslan Balkin <a href="mailto:baron@baron.su">baron@baron.su</a>
 * @version 1.0.0.0
 */
public class FakeBuildListener implements BuildListener {
	final PrintWriter printWriter = new PrintWriter(System.err);
	/**
	 * This writer will receive the output of the build
	 *
	 * @return must be non-null.
	 */
	@Override
	public PrintStream getLogger() {
		return System.out;
	}

	/**
	 * Annotates the current position in the output log by using the given annotation.
	 * If the implementation doesn't support annotated output log, this method might be no-op.
	 *
	 * @param ann
	 * @since 1.349
	 */
	@Override
	public void annotate(ConsoleNote ann) throws IOException {

	}

	/**
	 * Places a {@link HyperlinkNote} on the given text.
	 *
	 * @param url  If this starts with '/', it's interpreted as a path within the context path.
	 * @param text
	 */
	@Override
	public void hyperlink(String url, String text) throws IOException {

	}

	/**
	 * An error in the build.
	 *
	 * @param msg
	 * @return A writer to receive details of the error. Not null.
	 */
	@Override
	public PrintWriter error(String msg) {
		return new PrintWriter(System.err);
	}

	/**
	 * {@link Formatter#format(String, Object[])} version of {@link #error(String)}.
	 *
	 * @param format
	 * @param args
	 */
	@Override
	public PrintWriter error(String format, Object... args) {
		printWriter.printf(format, args);
		return printWriter;
	}

	/**
	 * A fatal error in the build.
	 *
	 * @param msg
	 * @return A writer to receive details of the error. Not null.
	 */
	@Override
	public PrintWriter fatalError(String msg) {
		printWriter.println(msg);
		return printWriter;
	}

	/**
	 * {@link Formatter#format(String, Object[])} version of {@link #fatalError(String)}.
	 *
	 * @param format
	 * @param args
	 */
	@Override
	public PrintWriter fatalError(String format, Object... args) {
		printWriter.printf(format, args);
		return printWriter;
	}

	/**
	 * Called when a build is started.
	 *
	 * @param causes Causes that started a build. See {@link Run#getCauses()}.
	 */
	@Override
	public void started(List<Cause> causes) {

	}

	/**
	 * Called when a build is finished.
	 *
	 * @param result
	 */
	@Override
	public void finished(Result result) {
		System.out.println("FINISHED BUILD: " + result.toExportedObject());
	}
}
