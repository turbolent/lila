package lila.test;

import static org.junit.Assert.*;

import java.util.Arrays;

import lila.runtime.LilaClass;
import lila.runtime.LilaObject;

import org.junit.Test;

public class C3 {

	LilaClass pane = new LilaClass(false, "pane", null);
	LilaClass scrollingMixin = new LilaClass(false, "scrollingMixin", null);
	LilaClass editingMixin = new LilaClass(false, "editingMixin", null);
	LilaClass scrollablePane =
		new LilaClass(false, "scrollablePane", null,
		              new LilaClass[] { pane, scrollingMixin });
	LilaClass editablePane =
		new LilaClass(false, "editablePane", null,
		              new LilaClass[] { pane, editingMixin });
	LilaClass editableScrollablePane =
		new LilaClass(false, "editableScrollablePane", null,
		              new LilaClass[] { scrollablePane, editablePane });

	@Test
	public void testLilaObject() {
		assertEquals(LilaObject.lilaClass.getAllSuperclasses(),
		             Arrays.asList(new LilaClass[] {
		     			LilaObject.lilaClass }));
	}

	@Test
	public void testPane() {
		assertEquals(pane.getAllSuperclasses(),
		             Arrays.asList(new LilaClass[] {
		            	 pane, LilaObject.lilaClass }));
	}

	@Test
	public void testScrollingMixin() {
		assertEquals(scrollingMixin.getAllSuperclasses(),
		             Arrays.asList(new LilaClass[] {
		     			scrollingMixin, LilaObject.lilaClass }));
	}

	@Test
	public void testEditingMixin() {
		assertEquals(editingMixin.getAllSuperclasses(),
		             Arrays.asList(new LilaClass[] {
		     			editingMixin, LilaObject.lilaClass }));
	}

	@Test
	public void testScrollablePane() {
		assertEquals(scrollablePane.getAllSuperclasses(),
		             Arrays.asList(new LilaClass[] {
		     			scrollablePane, pane, scrollingMixin,
		     			LilaObject.lilaClass }));
	}

	@Test
	public void testEditablePane() {
		assertEquals(editablePane.getAllSuperclasses(),
		             Arrays.asList(new LilaClass[] {
		     			editablePane, pane, editingMixin, LilaObject.lilaClass }));
	}

	@Test
	public void testEditableScrollablePane() {
		assertEquals(editableScrollablePane.getAllSuperclasses(),
		             Arrays.asList(new LilaClass[] {
		     			editableScrollablePane,
		     			scrollablePane, editablePane,
		     			pane, scrollingMixin, editingMixin,
		     			LilaObject.lilaClass }));
	}

}
