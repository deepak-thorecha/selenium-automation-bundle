package com.sysgears.seleniumbundle.core.pagemodel

import com.sysgears.seleniumbundle.core.uicomparison.AShotFactory
import com.sysgears.seleniumbundle.core.uicomparison.ScreenshotHandler
import com.sysgears.seleniumbundle.core.uicomparison.ScreenshotLoader
import com.sysgears.seleniumbundle.core.utils.AllureHelper
import ru.yandex.qatools.ashot.Screenshot

import java.awt.image.BufferedImage

abstract class AbstractPageWithUIComparison<T> extends AbstractPage<AbstractPageWithUIComparison> {

    /**
     * Instance of AllureHelper.
     */
    AllureHelper allure = new AllureHelper()

    /**
     * Instance of AShot factory.
     */
    AShotFactory aShotFactory = new AShotFactory(conf)

    /**
     * Instance of ScreenshotHandler.
     */
    ScreenshotHandler handler

    /**
     * Instance of ScreenshotLoader.
     */
    private ScreenshotLoader screenshotLoader = new ScreenshotLoader()

    /**
     * Generates paths to screenshots by a given screenshot name.
     *
     * @param screenshotName name of the screenshot
     *
     * @return Map of paths to screenshots
     */
    Map getPathsForScreenshot(String screenshotName) {
        conf.ui.path.collectEntries {
            [it.getKey(),
             "${it.getValue()}${File.separator}$os${File.separator}$browser${File.separator}${screenshotName}.png"]
        } as Map<String, String>
    }

    /**
     * Captures screenshot and compares it with the previously captured base screenshot. In case there are differences
     * saves the new screenshot and the image with marked discrepancies. In case there is no previously captured base
     * screenshot, saves the new screenshot and throws AssertionError.
     *
     * @param screenshotName name of the screenshot
     *
     * @return instance of the page
     *
     * @throws IOException is thrown if there is no baseline screenshot during comparison or file with ignored
     * elements wasn't found
     * @throws AssertionError is thrown if layout of the screenshot doesn't match to the baseline screenshot.
     */
    T compareLayout(String screenshotName) throws IOException, AssertionError {
        def fullPaths = getPathsForScreenshot(screenshotName)

        Screenshot screenshot = handler.capture()

        if (conf.baselineMode) {
            screenshotLoader.save(screenshot.getImage(), fullPaths.baseline)

            allure.attach("Baseline screenshot for: $screenshotName", screenshot.getImage())
        } else {
            try {
                def baseScreenshot = new Screenshot(screenshotLoader.retrieve(fullPaths.baseline))
                baseScreenshot.setIgnoredAreas(screenshot.getIgnoredAreas())

                BufferedImage markedImage = handler.compare(baseScreenshot, screenshot)

                if (markedImage) {
                    screenshotLoader.save(markedImage, fullPaths.difference)
                    screenshotLoader.save(screenshot.getImage(), fullPaths.actual)

                    allure.attach("Baseline screenshot for: $screenshotName", baseScreenshot.getImage())
                    allure.attach("Marked screenshot for: $screenshotName", markedImage)
                    allure.attach("Actual screenshot for: $screenshotName", screenshot.getImage())

                    log.info("Layout for: $screenshotName doesn't match to the base screenshot.")
                    throw new AssertionError("Layout for: $screenshotName doesn't match to the base screenshot.")
                } else {
                    log.info("Layout is identical for ${fullPaths.baseline}")
                }
            } catch (IOException e) {
                screenshotLoader.save(screenshot.getImage(), fullPaths.actual)
                log.info("New candidate screenshot has been made successfully: ${fullPaths.actual}", e)
                throw new IOException("No baseline screenshot found.", e)
            }
        }
        (T) this
    }
}