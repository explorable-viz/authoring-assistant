import { runTests, testURL, waitForFigure, waitFor } from "./shared/webtest-lib.js";

export const main = async () => {
    await runTests(testURL("figure-spm4b")([ (page) =>  waitForFigure(page)("")]))();
    await runTests(testURL("table-spm1")([ (page) => waitFor("#fig-output")(page) ]))();
    console.log("Success!");
};
