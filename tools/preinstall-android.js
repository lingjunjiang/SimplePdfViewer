if (process.version < 'v0.12')
{
    console.warn("Could not run SamplePdfViewer plugin android pre-install script");
    console.warn("You are running node " + process.version + "\nYou need to be running node 0.12 or above");
    console.warn("You won't be able to successfully run: cordova build");
}
else {
    console.log("Running SamplePdfViewer plugin android pre-install script");
    var execSync = require('child_process').execSync;
    execSync('cordova build android');
    console.log("Done SamplePdfViewer plugin android pre-install script");
}
