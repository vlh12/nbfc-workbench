import ReactDOM from "react-dom/client";

import AuthenticationEntry from "./components/AuthenticationEntry";

import "./styles.css";

const root = ReactDOM.createRoot(
  document.getElementById("root")!,
);

root.render(<AuthenticationEntry />);