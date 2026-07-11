import "./modules/code/campusPilot/campusPilot";

const container = document.getElementById("root");
const app = document.createElement("cp-campus-pilot");
app.enableAgent = true;
container.replaceChildren(app);