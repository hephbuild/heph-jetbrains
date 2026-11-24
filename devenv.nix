{ pkgs, ... }:

{
  # https://devenv.sh/basics/
  languages.java = {
    enable = true;
    jdk.package = pkgs.jdk21;
  };

  # Gradle comes from the checked-in wrapper.

  # https://devenv.sh/scripts/
  scripts.build.exec = "./gradlew buildPlugin";
  scripts.check.exec = "./gradlew test";
  scripts.verify.exec = "./gradlew verifyPlugin";
  scripts.ide.exec = "./gradlew runIde";

  enterShell = ''
    echo "heph-jetbrains dev env"
    java -version
  '';
}
