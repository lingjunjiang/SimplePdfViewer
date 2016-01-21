console.log("Running SamplePdfViewer plugin android post-install script");
var targetAndroidApi = 22; 


//--------------------------------------
// Useful functions
//--------------------------------------
var fs = require('fs');
var exec = require('child_process').exec;
var path = require('path');

console.log("Current PWD:" + __dirname);
var projectFolder = __dirname.slice(0,__dirname.indexOf('plugins'));
console.log("Main Folder:" + projectFolder);

function copyFileSync( source, target ) {

    var targetFile = target;
    console.log("Target File:" + targetFile);
    
    console.log("Base Name:" + path.basename( source ));

    //if target is a directory a new file with the same name will be created
    if ( fs.existsSync( target ) ) {
        
        if ( fs.lstatSync( target ).isDirectory() ) {
            targetFile = path.join( target, path.basename( source ) );
            console.log("Add Base Target File:" + targetFile);
        }
    }

    fs.writeFileSync(targetFile, fs.readFileSync(source));
};

function copyFolderRecursiveSync( source, target ) {
    var files = [];

    //check if folder needs to be created or integrated
    var targetFolder = path.join( target, path.basename( source ) );
    if ( !fs.existsSync( targetFolder ) ) {
        fs.mkdirSync( targetFolder );
    }

    //copy
    if ( fs.lstatSync( source ).isDirectory() ) {
        files = fs.readdirSync( source );
        console.log(files);
        files.forEach( function ( file ) {
            var curSource = path.join( source, file );
            if ( fs.lstatSync( curSource ).isDirectory() ) {
                console.log(curSource + "is Directory" );
                copyFolderRecursiveSync( curSource, targetFolder );
            } else {
                console.log(curSource + "is File" );
                copyFileSync( curSource, targetFolder );
            }
        } );
    }
};

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

var libProject = path.join(projectFolder,'platforms', 'android','mupdf');

var srcLibProject = path.join(projectFolder,'plugins', 'cordova-plugin-SimplePdfViewer', 'src', 'android', 'libs', 'mupdf');
var destLibProject = path.join(projectFolder,'platforms', 'android');

console.log('Copying mupdf library into android project folder');
copyFolderRecursiveSync(srcLibProject,destLibProject);

console.log('Fixing application project.properties');
fixFile(path.join('platforms', 'android', 'project.properties'), fixProjectProperties);

console.log('Updating application to use mupdf' + ' library project ');
exec(androidExePath + ' update project -p . -t "android-' + targetAndroidApi + '" -l ' + 'mupdf', {cwd: path.resolve(process.cwd(), path.join('platforms', 'android'))});

console.log("Done running SamplePdfViewer plugin android post-install script");
