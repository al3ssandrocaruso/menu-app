#!/usr/bin/env bash
#
# Re-records the API fixtures used by the offline unit tests.
#
# Unit tests never hit the network, so when the published data set changes these recordings must
# be refreshed deliberately. Run this, re-run the tests, and commit the diff.
#
#   ./scripts/refresh-fixtures.sh
#
set -euo pipefail

BASE_URL="${MENUAPP_DATA_BASE_URL:-https://raw.githubusercontent.com/al3ssandrocaruso/restaurantsappdata/main}"
FIXTURES="$(cd "$(dirname "$0")/.." && pwd)/app/src/test/resources/fixtures"

mkdir -p "$FIXTURES"

echo "Fetching previews from $BASE_URL"
curl --fail --silent --show-error "$BASE_URL/restaurants/allpreviews" -o "$FIXTURES/allpreviews.json"

# Record a menu for each restaurant the previews advertise.
ids=$(python3 -c "import json,sys; print(' '.join(r['id'] for r in json.load(open('$FIXTURES/allpreviews.json'))['Restaurants']))")

for id in $ids; do
  echo "Fetching menu $id"
  curl --fail --silent --show-error "$BASE_URL/menus/$id" -o "$FIXTURES/menu_$id.json"
done

echo
echo "Recorded: $(ls -1 "$FIXTURES" | tr '\n' ' ')"
echo "Now run: ./gradlew testDebugUnitTest"
