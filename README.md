# Monthly Expense Tracker (Java + MongoDB + Docker)

A console Java app that:
- Adds expenses (category, amount, description, date)
- Calculates and shows monthly totals
- Shows category-wise summary
- Saves/reads everything from MongoDB

## Project layout
```
expense-tracker/
├── pom.xml
├── Dockerfile
├── .dockerignore
├── docker-compose.yml          (optional convenience option)
└── src/main/java/com/expensetracker/
    ├── Main.java
    └── db/MongoDBConnection.java
```

---

## Option A — Pure Dockerfile (what you asked for)

This requires MongoDB to be reachable from the app container. Easiest way: run Mongo in
its own container on a shared Docker network.

```bash
# 1. Build the app image
cd expense-tracker
docker build -t expense-tracker .

# 2. Create a shared network (so containers can resolve each other by name)
docker network create expense-net

# 3. Run MongoDB on that network
docker run -d --name mongo --network expense-net -p 27017:27017 mongo:7

# 4. Run the app container interactively (-it is required since this is a console app)
docker run -it --rm \
  --network expense-net \
  -e MONGO_URI=mongodb://mongo:27017 \
  --name expense-app \
  expense-tracker
```

Notes:
- `-it` is required — without it, the app's `Scanner` input won't work and the menu will hang/fail.
- `--network expense-net` lets the app container resolve the hostname `mongo` to the Mongo container's IP.
- `MONGO_URI` and `MONGO_DB_NAME` are read from environment variables (see `MongoDBConnection.java`), so you can point at any Mongo instance without rebuilding the image.
- To clean up: `docker rm -f expense-app mongo && docker network rm expense-net`

## Option B — docker-compose (one command, included as a bonus)

```bash
docker compose up --build
```

This builds the app image and starts both containers on a shared network automatically.
To interact with the running app's console after `up`, run:
```bash
docker attach expense-app
```
Stop everything with `docker compose down` (add `-v` to also wipe the Mongo data volume).

---

## Verifying data is actually in MongoDB

```bash
docker exec -it mongo mongosh expense_tracker_db --eval "db.expenses.find().pretty()"
```

---

## How the menu works
```
1. Add Expense              -> prompts for category, amount, description, date
2. View All Expenses        -> lists every record + grand total
3. View Monthly Total       -> enter month + year, get the sum for that period
4. View Category-wise Summary -> totals grouped by category
5. Delete Expense           -> delete by the MongoDB _id shown in option 2
6. Exit
```
