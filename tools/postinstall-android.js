console.log("Running SamplePdfViewer plugin android post-install script");
var targetAndroidApi = 22; 


//--------------------------------------
// Useful functions
//--------------------------------------
var fs = require('fs');
var exec = require('child_process').exec;
var path = require('path');

var copyFile = function(srcPath, targetPath) {
    fs.createReadStream(srcPath).pipe(fs.createWriteStream(targetPath));
};

var fixFile = function(path, fix) {
    fs.readFile(path, 'utf8', function (err, data) { 
        fs.writeFile(path, fix(data), function (err) {         
            if (err) { 
                console.log(err); 
            } 
        });
    });
};

// Function to removes current cordova library project reference 
var fixSDKProjectProperties = function(data) {
    return data.replace(/android\.library\.reference.*cordova\/framework\n/, '');
};

// Function to manifest merger
var fixProjectProperties = function(data) {
    return data + "manifestmerger.enabled=true\n";
};

var getAndroidSDKToolPath = function() {
    var androidHomeDir = process.env.ANDROID_HOME;
    if (typeof androidHomeDir !== 'string') {
        console.log('You must set the ANDROID_HOME environment variable to the path of your installation of the Android SDK.');
        return null;
    }

    var androidExePath = path.join(androidHomeDir, 'tools', 'android');
    var isWindows = (/^win/i).test(process.platform);
    if (isWindows) {
        androidExePath = androidExePath + '.bat';
    }
    if (!fs.existsSync(androidExePath)) {
        console.log('The "android" utility does not exist at ' + androidExePath + '.  Make sure you\'ve properly installed the Android SDK.');
        return null;
    }

    return androidExePath;
};

//--------------------------------------
// Doing actual post installation work
//--------------------------------------
var androidExePath = getAndroidSDKToolPath();
if (androidExePath === null) {
    process.exit(2);
}

var libProject = path.join('..', '..', 'plugins', 'cordova-plugin-SimplePdfViewer', 'src', 'android', 'libs', 'mupdf');

console.log('Fixing application project.properties');
fixFile(path.join('platforms', 'android', 'project.properties'), fixProjectProperties);

console.log('Updating application to use mupdf' + ' library project ');
exec(androidExePath + ' update project -p . -t "android-' + targetAndroidApi + '" -l ' + libProject, {cwd: path.resolve(process.cwd(), path.join('platforms', 'android'))});

console.log("Done running SamplePdfViewer plugin android post-install script");
