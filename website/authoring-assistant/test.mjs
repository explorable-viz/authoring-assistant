import { runTests, testURL, waitFor } from "./shared/webtest-lib.js"

export const main = async () => {
    await runTests(testURL("scigen-manual-old-1002.0773")([ page => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual-old-1003.0206")([ page => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual-old-1110.3094")([ page => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual-old-1805.02474v1-10")([ page => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual-old-1805.02474v1-14")([ (page) => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual-old-1807.07279v3-23")([ (page) => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual-old-1904.12550v1-5")([ (page) => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual-old-1904.12550v1-6")([ (page) => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual-old-1906.02780v1-17")([ (page) => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual-old-ipcc")([ (page) => waitFor("div#fig-output")(page) ]))()
    console.log("Success!")
}
