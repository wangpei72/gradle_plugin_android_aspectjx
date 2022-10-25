package com.lancewu.aspectj.testlibrary;

/**
 * Created by LanceWu on 2022/10/24.<br>
 * java文件测试
 */
public class LibraryJavaTest {

    public void test() {
        new TestLibrary().printLog("call from LibraryJavaTest");
    }
}
