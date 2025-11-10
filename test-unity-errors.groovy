pipeline {
    agent any

    stages {
        stage('Generate Unity Build Logs') {
            steps {
                script {
                    // Generate realistic Unity build output with embedded errors
                    sh '''
#!/bin/bash

echo "========================================"
echo "Unity Build Test - 500 Lines with Errors"
echo "========================================"
echo ""
echo "Unity Editor version: 2023.2.1f1"
echo "Build path: /Users/jenkins/Unity/Builds/iOS"
echo "Target platform: iOS"
echo ""

# Normal build output (lines 1-50)
for i in {1..50}; do
  echo "DisplayProgressbar: Building Player, step $i/500"
done

echo ""
echo "--- Unity Compiler Phase ---"
echo "Compiling assets..."
echo "Processing shader variants..."

# ERROR 1: Unity Error with colon
echo "Error : Failed to compile shader 'Standard'"

for i in {51..80}; do
  echo "Compiling script $i..."
done

# ERROR 2: CompilerError
echo "CompilerError in Assets/Scripts/PlayerController.cs(45,12): syntax error"

for i in {81..100}; do
  echo "Building scene $i..."
done

# ERROR 3: BuildFailedException
echo "UnityEditor.BuildFailedException: Build failed with 3 errors"

for i in {101..120}; do
  echo "Processing asset bundle $i..."
done

# ERROR 4: UnityException
echo "UnityException: Failed to load scene 'MainMenu'"

for i in {121..140}; do
  echo "Optimizing textures $i..."
done

# ERROR 5: C# script error with line/column
echo "Assets/Scripts/GameManager.cs(123,45): error CS0246: The type or namespace name could not be found"

for i in {141..150}; do
  echo "Validating scene data $i..."
done

# ERROR 6: Generic Exception (catches NullReferenceException, etc.)
echo "NullReferenceException: Object reference not set to an instance of an object"
echo "  at PlayerController.Update() in Assets/Scripts/PlayerController.cs:78"

echo ""
echo "--- Xcode Build Phase ---"
echo "Building Xcode project..."
echo "Running xcodebuild..."

for i in {151..170}; do
  echo "Compiling source file $i.m..."
done

# ERROR 7: BUILD FAILED
echo "** BUILD FAILED **"

for i in {171..190}; do
  echo "Cleaning up build artifacts $i..."
done

# ERROR 8: ld linker error
echo "ld: error: undefined symbol: _OBJC_CLASS_\$_UnityAppController"

for i in {191..210}; do
  echo "Linking framework $i..."
done

# ERROR 9: clang error
echo "clang: error: linker command failed with exit code 1"

for i in {211..230}; do
  echo "Processing library $i..."
done

# ERROR 10: Build input file error
echo "error: Build input file cannot be found: '/Users/jenkins/Unity/Builds/iOS/Libraries/libiPhone-lib.a'"

for i in {231..250}; do
  echo "Validating build configuration $i..."
done

# ERROR 11: Emoji error marker
echo "❌ error: Could not find module 'UnityFramework' for target 'Unity-iPhone'"

for i in {251..270}; do
  echo "Archiving build artifacts $i..."
done

# ERROR 12: Generic error with non-space
echo "error: The sandbox is not in sync with the Podfile.lock"

echo ""
echo "--- Android/Gradle Build Phase ---"
echo "Building Android APK..."
echo "Running Gradle tasks..."

for i in {271..290}; do
  echo ":app:compileDebugJavaWithJavac - task $i"
done

# ERROR 13: BUILD FAILED
echo "BUILD FAILED in 2m 34s"

for i in {291..310}; do
  echo "Processing dependency $i..."
done

# ERROR 14: FAILURE Build failed
echo "FAILURE: Build failed with an exception."

for i in {311..330}; do
  echo "Resolving configuration $i..."
done

# ERROR 15: Task FAILED
echo "> Task :app:mergeDebugResources FAILED"

for i in {331..350}; do
  echo "Merging resources $i..."
done

# ERROR 16: Execution failed for task
echo "Execution failed for task ':app:processDebugManifest'."

for i in {351..370}; do
  echo "Processing manifest entries $i..."
done

# ERROR 17: What went wrong
echo "* What went wrong:"
echo "A problem occurred configuring project ':app'."

for i in {371..390}; do
  echo "Configuring project settings $i..."
done

# ERROR 18: Package does not exist
echo "error: package com.unity3d.player does not exist"

for i in {391..410}; do
  echo "Compiling Java source $i..."
done

# ERROR 19: Cannot find symbol
echo "error: cannot find symbol MainActivity"

for i in {411..430}; do
  echo "Generating R.java file $i..."
done

# ERROR 20: Make error
echo "make: *** [all] Error 2"

for i in {431..450}; do
  echo "Running native build $i..."
done

# ERROR 21: Make with index error
echo "make[1]: *** [CMakeFiles/game.dir/build.make:76: CMakeFiles/game.dir/src/main.cpp.o] Error 1"

for i in {451..470}; do
  echo "Compiling native library $i..."
done

# ERROR 22: Ninja build stopped
echo "ninja: build stopped: subcommand failed."

# Fill remaining lines with normal output
for i in {461..490}; do
  echo "Finalizing build artifacts $i..."
done

echo ""
echo "--- Build Summary ---"
echo "Total errors: 21"
echo "Total warnings: 0"
echo "Build duration: 5m 23s"
echo "Build FAILED"
echo ""

# Add a few more lines to reach 500
echo "Cleaning up workspace..."
echo "Removing temporary files..."
echo "Archiving logs..."
echo "Sending notifications..."
echo "Build process complete (failed)"
echo ""
echo "========================================"
echo "End of build log"
echo "========================================"

exit 1
                    '''
                }
            }
        }

        stage('Analyze Errors') {
            steps {
                script {
                    try {
                        echo "Previous stage failed as expected"
                    } catch (Exception e) {
                        echo "Attempting to analyze errors..."
                        // Patterns will be read from job configuration in the UI
                        analyzeError(
                            errorPatterns: '''(?i)\\bError\\s*:\\s*
(?i)\\bCompilerError\\b
(?i)\\bBuildFailedException\\b
(?i)\\bUnityException\\b
(?i)Assets/.*\\.cs\\(\\d+,\\d+\\):\\s*error
(?i)\\bException\\b
(?i)^\\s*\\*\\*\\s*BUILD FAILED\\s*\\*\\*
(?i)ld:\\s*error:
(?i)clang:\\s*error:
(?i)error:\\s*(?:linker command failed|Build input file cannot be found)
(?i)❌\\s*.*error
(?i)\\berror:\\s*[^\\s]
(?i)^BUILD FAILED
(?i)^FAILURE:.*Build failed
(?i)^>\\s*Task\\s+\\S+\\s+FAILED$
(?i)^Execution failed for task
(?i)\\*\\s*What went wrong:
(?i)error:\\s*package\\s+\\S+\\s+does not exist
(?i)error:\\s*cannot find symbol
(?i)^make:\\s+\\*\\*\\*.*Error\\s+\\d+
(?i)^make\\[\\d+\\]:\\s+\\*\\*\\*.*Error\\s+\\d+
(?i)^ninja:\\s+build stopped:''',
                            maxLines: 200
                        )
                        throw e
                    }
                }
            }
        }
    }

    post {
        failure {
            script {
                echo "Build failed - check the 'Analyze Error' button on the console page"
                echo "Or view the 'AI Error Analysis' link in the sidebar if analyzeError() was called"
            }
        }
    }
}
