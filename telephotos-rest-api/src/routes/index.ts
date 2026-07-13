import { Hono } from "hono"
import auth from "./auth"
import media from "./media"
import stateRoute from "./state"

const routes = new Hono()

routes.route("/auth", auth)
routes.route("/media", media)
routes.route("/state", stateRoute)

export default routes
