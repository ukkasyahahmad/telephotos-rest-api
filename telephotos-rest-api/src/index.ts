import { Hono } from "hono"
import { cors } from "hono/cors"
import { config } from "./config"
import routes from "./routes"
import { readFileSync } from "fs"

const appJs = readFileSync("./public/app.js", "utf-8")

const app = new Hono()

app.use("/*", cors())

app.get("/app.js", (c) =>
  c.newResponse(appJs, 200, { "Content-Type": "application/javascript" })
)

app.get("/", (c) => c.html(`
  <!DOCTYPE html>
  <html lang="en">
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TelePhotos</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>📸</text></svg>">
  </head>
  <body class="bg-gray-950 text-gray-100 min-h-screen">
    <div id="app"></div>
    <script src="/app.js"></script>
  </body>
  </html>
`))

app.route("/api", routes)

export default {
  port: config.port,
  fetch: app.fetch,
}
