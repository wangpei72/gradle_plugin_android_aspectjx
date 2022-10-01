package com.lancewu.aspectj;

import com.lancewu.aspectj.testlibrary.TestLibrary;

/**
 * Created by LanceWu on 2022/9/29.<br>
 * Java文件测试
 */
public class AppJavaFileTest {

    public enum Type {
        TEST_1,
        TEST_2
    }

    public void test() {
        new TestLibrary().printLog("call from AppJavaFileTest");
    }

    private class Inner {

        public void test() {
            new TestLibrary().printLog("call from AppJavaFileTest.Inner");
        }
    }

    public static class StaticInner {

        public void test() {
            new TestLibrary().printLog("call from AppJavaFileTest.StaticInner");
        }
    }

}
