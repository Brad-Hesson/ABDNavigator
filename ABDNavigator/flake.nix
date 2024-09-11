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
      java = pkgs.jdk21.override { enableJavaFX = true; };
      settingsFile = pkgs.writeText "settings.json" (builtins.toJSON {
        "java.jdt.ls.java.home" = "${java}";
        "java.configuration.runtimes" = [
          {
            "path" = "${java}";
            "name" = "nix-jdk";
          }
        ];
      });
    in
    {
      devShell = with pkgs; mkShell {
        packages = [
          pkgs.jdk21.override
          { enableJavaFX = true; }
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

