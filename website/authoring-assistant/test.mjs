import { runTests, testURL, waitFor } from "./shared/webtest-lib.js"

export const main = async () => {
    await runTests(testURL("scigen-manual/1002.0773-8586")([ page => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual/1003.0206-7624")([ page => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual/1110.3094-5931")([ page => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual/1805.02474v1-10")([ page => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual/1805.02474v1-14")([ (page) => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual/1807.07279v3-23")([ (page) => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual/1904.12550v1-5")([ (page) => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual/1904.12550v1-6")([ (page) => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual/1906.02780v1-17")([ (page) => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual/1805.02474v1-10")([ page => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-manual/1905.05979v2-88")([ page => waitFor("div#fig-output")(page) ]))()
    console.log("Success!")
}
