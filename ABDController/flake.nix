{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };
  outputs = flakes: flakes.flake-utils.lib.eachDefaultSystem (system:
    let
      pkgs = import flakes.nixpkgs {
        inherit system;
        config.allowUnfree = true;
      };
      java = pkgs.jdk;
      settingsFile = pkgs.writeText "settings.json" (builtins.toJSON {
        "java.jdt.ls.java.home" = "${java}";
        "java.configuration.runtimes" = [
          {
            "path" = "${java}/lib/openjdk";
            "name" = "JavaSE-21";
            "default" = true;
          }
        ];
        "java.configuration.detectJdksAtStart" = false;
      });
    in
    {
      devShell = with pkgs; mkShell {
        packages = [
        ];
        buildInputs = [
        ];
        shellHook = ''
          mkdir .vscode
          cp -f ${settingsFile} .vscode/settings.json
        '';
      };
    }
  );
}

