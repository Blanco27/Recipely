"""
Seed Recipely demo data straight into a pulled Room DB (no app seed code, no on-device sqlite3).

Usage:
  1. Reset + pull a fresh DB so the schema/identity hash match the installed build:
       adb shell pm clear com.nwe.recipely
       adb shell am start -n com.nwe.recipely/.MainActivity ; (wait) ; adb shell am force-stop com.nwe.recipely
       adb exec-out run-as com.nwe.recipely cat databases/recipely.db > seed.db
  2. Point DB below at that file, sanity-check the column list against data/RecipeEntities.kt, run.
  3. Push back (from PowerShell, not git-bash):
       adb push seed.db /data/local/tmp/seed.db
       adb shell run-as com.nwe.recipely cp /data/local/tmp/seed.db databases/recipely.db
       adb shell run-as com.nwe.recipely rm -f databases/recipely.db-wal databases/recipely.db-shm

Images (optional): download Unsplash photos, push into files/images/, and set the imageUri fields
below to the DEVICE path so Coil's File(path) can load them.
"""
import sqlite3

DB = r"C:/Users/nikla/AppData/Local/Temp/recipely_shots/seed.db"  # the pulled DB
IMG = "/data/user/0/com.nwe.recipely/files/images"               # device path; set to None to skip images

def img(name):
    return f"{IMG}/{name}" if IMG else None

# (name, imageUri, prepMin, servings, kcal, carbs_g, protein_g, fat_g, category, [ingredients], [steps])
# Spread categories (fills the filter bar); leave one recipe without nutrition (shows it's optional);
# keep the hero recipe fully populated for the detail screenshot.
recipes = [
    ("Banana Bread", img("banana.jpg"), 60, 8, None, None, None, None, "BAKING",
     ["3 ripe bananas", "250 g flour", "120 g sugar", "100 g butter", "2 eggs",
      "1 tsp baking soda", "Pinch of salt"],
     ["Mash the bananas in a bowl.",
      "Cream the butter with the sugar, then beat in the eggs.",
      "Fold in the flour, baking soda and salt, then the bananas.",
      "Pour into a loaf tin and bake at 180 °C for about 50 minutes."]),
    ("Berry Pancakes", img("pancakes.jpg"), 25, 2, 540, 76.0, 15.0, 16.0, "BREAKFAST",
     ["150 g flour", "200 ml milk", "1 egg", "1 tbsp sugar", "1 tsp baking powder",
      "150 g mixed berries", "Butter for the pan"],
     ["Whisk flour, milk, egg, sugar and baking powder into a smooth batter.",
      "Gently fold in half of the berries.",
      "Fry small pancakes in butter until golden on both sides.",
      "Serve stacked and topped with the remaining berries."]),
    ("Greek Salad", img("salad.jpg"), 15, 2, 280, 14.0, 9.0, 22.0, "SALAD",
     ["3 tomatoes", "1 cucumber", "1 red onion", "100 g feta", "A handful of black olives",
      "3 tbsp olive oil", "1 tsp dried oregano"],
     ["Cut the tomatoes, cucumber and onion into rough chunks.",
      "Add the olives and crumble the feta over the top.",
      "Drizzle with olive oil and sprinkle with oregano."]),
    ("Oven Vegetables with Feta", img("veg.jpg"), 35, 3, 480, 28.0, 18.0, 30.0, "MAIN",
     ["2 bell peppers", "1 zucchini", "1 red onion", "200 g cherry tomatoes", "200 g feta",
      "3 tbsp olive oil", "Salt & pepper"],
     ["Cut all the vegetables into bite-sized pieces.",
      "Spread on a tray and set the block of feta in the middle.",
      "Drizzle with olive oil and season with salt and pepper.",
      "Roast at 200 °C for about 25 minutes."]),
    ("Spaghetti Carbonara", img("carbonara.jpg"), 30, 2, 820, 78.0, 34.0, 38.0, "MAIN",  # hero
     ["200 g spaghetti", "100 g pancetta", "2 eggs + 1 yolk", "50 g pecorino", "Black pepper"],
     ["Cook the spaghetti al dente in plenty of salted water.",
      "Fry the pancetta in a pan until crisp.",
      "Whisk the eggs with the grated pecorino and plenty of pepper.",
      "Drain the pasta and quickly toss with the pancetta and egg mixture."]),
]

c = sqlite3.connect(DB)
c.execute("PRAGMA foreign_keys=ON")
for t in ("steps", "ingredients", "recipes"):
    c.execute(f"DELETE FROM {t}")
for (name, image, prep, serv, kcal, carbs, prot, fat, cat, ings, steps) in recipes:
    rid = c.execute(
        "INSERT INTO recipes(name,imageUri,prepTimeMinutes,servings,calories,"
        "carbsGrams,proteinGrams,fatGrams,category) VALUES(?,?,?,?,?,?,?,?,?)",
        (name, image, prep, serv, kcal, carbs, prot, fat, cat)).lastrowid
    for i, txt in enumerate(ings):
        c.execute("INSERT INTO ingredients(recipeId,text,position) VALUES(?,?,?)", (rid, txt, i))
    for i, txt in enumerate(steps):
        c.execute("INSERT INTO steps(recipeId,text,imageUri,position) VALUES(?,?,?,?)",
                  (rid, txt, None, i))
c.commit()
c.execute("PRAGMA wal_checkpoint(TRUNCATE)")  # merge WAL so a single .db file is enough to push
c.commit()
print("recipes:", c.execute("SELECT count(*) FROM recipes").fetchone()[0])
for r in c.execute("SELECT name,category,calories,servings FROM recipes ORDER BY name"):
    print(" -", r)
c.close()
