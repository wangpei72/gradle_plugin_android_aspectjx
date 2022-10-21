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
        String content = switchTest(CASE_1);
        new TestLibrary().printLog("call from AppJavaFileTest, switchTest result:" + content);
    }

    private static final int CASE_1 = Integer.MAX_VALUE;
    private static final int CASE_2 = Integer.MAX_VALUE - 1;
    private static final int CASE_3 = Integer.MAX_VALUE - 2;

    public String switchTest(int i) {
        String content = "";
        switch (i) {
            case CASE_1:
                content = "CASE_1";
                break;
            case CASE_2:
                content = "CASE_2";
                break;
            case CASE_3:
                content = "CASE_3";
                break;
            default:
                break;
        }
        return content;
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
