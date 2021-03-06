/*    Copyright 2013-2015 ARM Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/


package com.arm.wlauto.uiauto.glbenchmark;

import android.app.Activity;
import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.util.Log;

import com.arm.wlauto.uiauto.BaseUiAutomation;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class UiAutomation extends BaseUiAutomation {

    public static String TAG = "glb";
    public static int maxScrolls = 15;

@Test
public void runUiAutomation() throws Exception {
        initialize_instrumentation();
        Bundle parameters = getParams();
        String version = parameters.getString("version");
        String useCase = parameters.getString("use_case").replace('_', ' ');
        String variant = parameters.getString("variant").replace('_', ' ');
        int iterations = parameters.getInt("iterations");
        int testTimeoutSeconds = parameters.getInt("timeout");
        if (iterations < 1)
                iterations = 1;

        goToPreformanceTestsMenu();
        selectUseCase(version, useCase, variant);
        hitStart();
        waitForResults(version, useCase, testTimeoutSeconds);
        extractResults();
        iterations -= 1;

        while (iterations > 0) {
                mDevice.pressBack();
                goToPreformanceTestsMenu();
                hitStart();
                waitForResults(version, useCase, testTimeoutSeconds);
                extractResults();
                iterations -= 1;
        }

        Bundle status = new Bundle();
        mInstrumentation.sendStatus(Activity.RESULT_OK, status);
    }

    public void goToPreformanceTestsMenu() throws Exception {
        UiSelector selector = new UiSelector();
        UiObject choosePerfTest = mDevice.findObject(selector.text("Performance Tests")
                                                       .className("android.widget.TextView"));
        choosePerfTest.clickAndWaitForNewWindow();
    }

    public void selectUseCase(String version, String useCase, String variant) throws Exception {
        UiSelector selector = new UiSelector();
        UiScrollable testList = new UiScrollable(selector.className("android.widget.ListView"));
        UiObject useCaseText = mDevice.findObject(selector.className("android.widget.TextView")
                                                    .text(useCase)
                                           );
        if (version.equals("2.7")){
                UiObject variantText =  useCaseText.getFromParent(selector.className("android.widget.TextView")
                                                                          .text(variant));
                int scrolls = 0;
                while(!variantText.exists()) {
                        testList.scrollForward();
                        scrolls += 1;
                        if (scrolls >= maxScrolls) {
                                break;
                        }
                }
                variantText.click();
        }
        else if (version.equals("2.5")){
                int scrolls = 0;
                while(!useCaseText.exists()) {
                        testList.scrollForward();
                        scrolls += 1;
                        if (scrolls >= maxScrolls) {
                                break;
                        }
                }
                useCaseText.click();
                UiObject modeDisableModeButton = null;
                if (variant.contains("Onscreen"))
                        modeDisableModeButton = mDevice.findObject(selector.text("Offscreen"));
                else
                        modeDisableModeButton = mDevice.findObject(selector.text("Onscreen"));
                modeDisableModeButton.click();
        }
    }

    public void hitStart() throws Exception {
        UiSelector selector = new UiSelector();
        UiObject startButton = mDevice.findObject(selector.text("Start"));
        startButton.clickAndWaitForNewWindow();
    }

    public void waitForResults(String version, String useCase, int timeout) throws Exception {
        UiSelector selector = new UiSelector();
        UiObject results = null;
        if (version.equals("2.7"))
                results = mDevice.findObject(selector.text("Results").className("android.widget.TextView"));
        else
                results =  mDevice.findObject(selector.text(useCase).className("android.widget.TextView"));
	Log.v(TAG, "Waiting for results screen.");
	// On some devices, the results screen sometimes gets "backgrounded" (or
	// rather, doesn't seem to come to foreground to begin with). This code
	// attemps to deal with that by explicitly bringing glbench to the
	// foreground if results screen doesn't appear within testTimeoutSeconds seconds of
	// starting GLB.
        if (!results.waitForExists(TimeUnit.SECONDS.toMillis(timeout))) {
		Log.v(TAG, "Results screen not found. Attempting to bring to foreground.");
		String[] commandLine = {"am", "start",
					"-a", "android.intent.action.MAIN",
					"-c", "android.intent.category.LAUNCHER",
					"-n", "com.glbenchmark.glbenchmark27/com.glbenchmark.activities.GLBenchmarkDownloaderActivity"};
		Process proc = Runtime.getRuntime().exec(commandLine);
		proc.waitFor();
		Log.v(TAG, String.format("am start exit value: %d", proc.exitValue()));
		if (!results.exists()) {
			throw new UiObjectNotFoundException("Could not find results screen.");
		}
	}
	Log.v(TAG, "Results screen found.");
    }

    public void extractResults() throws Exception {
            Log.v(TAG, "Extracting results.");
	    sleep(2); // wait for the results screen to fully load.
            UiSelector selector = new UiSelector();
            UiObject fpsText = mDevice.findObject(selector.className("android.widget.TextView")
                                                    .textContains("fps")
                                           );
            UiObject otherText = fpsText.getFromParent(selector.className("android.widget.TextView").index(0));

            Log.v(TAG, String.format("GLBenchmark metric: %s", otherText.getText().replace('\n', ' ')));
            Log.v(TAG, String.format("GLBenchmark FPS: %s", fpsText.getText().replace('\n', ' ')));
    }
}
