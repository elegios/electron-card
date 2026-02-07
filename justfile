build:
    clj -M -m cljs.main --compile-opts '{:asset-path "./js/"}' --output-dir ./resources/js/ --compile electron_card.app

run:
    electron ./resources
