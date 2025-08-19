import { runTests, testURL, waitFor } from "./shared/webtest-lib.js";

export const main = async () => {
    await runTests(testURL("figure-spm4b")([ (page) =>  waitFor("svg")(page)]))();
    await runTests(testURL("table-spm1")([ (page) => waitFor("#fig-output")(page) ]))();
    console.log("Success!");
};
