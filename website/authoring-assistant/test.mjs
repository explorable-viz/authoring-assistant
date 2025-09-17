import { runTests, testURL, waitFor } from "./shared/webtest-lib.js"

export const main = async () => {
    await runTests(testURL("scigen-1805.02474v1-10")([ page => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-1805.02474v1-14")([ (page) => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-1904.12550v1-5")([ (page) => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-1904.12550v1-6")([ (page) => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-1906.02780v1-17")([ (page) => waitFor("div#fig-output")(page) ]))()
    await runTests(testURL("scigen-ipcc")([ (page) => waitFor("div#fig-output")(page) ]))()
    console.log("Success!")
}
