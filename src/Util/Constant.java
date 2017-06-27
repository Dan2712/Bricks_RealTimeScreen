package Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;

import NodeSelection.tree.BasicTreeNode;

public class Constant {
	private static final String ROOT = System.getProperty("user.dir");
	
	public static Document document;
	public static List<BasicTreeNode>  checkList = new ArrayList<BasicTreeNode>();;

	public static File getMinicap() {
		return new File(ROOT, "minicap");
	}

	public static File getMinicapBin() {
		return new File(ROOT, "minicap/bin");
	}

	public static File getMinicapSo() {
		return new File(ROOT, "minicap/shared");
	}

}
