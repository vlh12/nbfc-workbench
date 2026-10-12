import ReactDOM from "react-dom/client";
import { auth } from "./auth";
import App from "./App";
import "./styles.css";
const root = ReactDOM.createRoot(document.getElementById("root")!);
auth.init()
  .then(() => root.render(<App />))
  .catch(() => root.render(<main className="mx-auto max-w-xl p-10"><h1 className="text-2xl font-bold">Demo could not start</h1><p className="my-4">Allow browser session storage for this local application, then retry.</p><button onClick={() => location.reload()} className="rounded bg-teal-800 px-4 py-2 text-white">Retry</button></main>));
