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
      settingsFile = pkgs.writeText "settings.json" (builtins.toJSON {
        "java.jdt.ls.java.home" = "${pkgs.jdk}";
      });
    in
    {
      devShell = with pkgs; mkShell {
        packages = [
        ];
        JRE_PATH = pkgs.jre8;
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

