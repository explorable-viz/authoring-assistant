import { runTests, testURL, waitForFigure, waitFor } from "./shared/webtest-lib.js";

export const main = async () => {
    await runTests(testURL("figure-spm4b")([ (page) => waitForFigure(page)("fig-leftBarChart") ]))();
    await runTests(testURL("table-spm1")([ (page) => waitFor("text#fig-explanation119")(page) ]))();
    console.log("Success!");
};
