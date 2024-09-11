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
      settings = {
        "java.jdt.ls.java.home" = pkgs.jdk;
      };
      settingsFile = pkgs.writeText "settings.json" (builtins.toJSON settings);
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

