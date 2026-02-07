{
  description = "Small clojurescript program for generating cards programmatically.";

  inputs = {
    nixpkgs.url = "https://flakehub.com/f/NixOS/nixpkgs/0.1";
  };

  outputs = { self, nixpkgs }:
    let pkgs = nixpkgs.legacyPackages.x86_64-linux.pkgs; in
    {
      devShells.x86_64-linux.default = pkgs.mkShell {
        name = "dev shell";
        buildInputs = [
          pkgs.clojure
          pkgs.just
          pkgs.electron
        ];
      };
    };
}
