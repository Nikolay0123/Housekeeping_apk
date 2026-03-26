async function api(path, { method = "GET", body } = {}) {
  const opts = { method, headers: {} };
  if (body !== undefined) {
    opts.headers["Content-Type"] = "application/json";
    opts.body = JSON.stringify(body);
  }
  const res = await fetch(path, opts);
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    const err = new Error(data.error || "REQUEST_FAILED");
    err.data = data;
    throw err;
  }
  return data;
}

function el(tag, attrs = {}, ...children) {
  const n = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (k === "class") n.className = v;
    else if (k === "onclick") n.onclick = v;
    else if (k.startsWith("on")) n.addEventListener(k.slice(2), v);
    else if (v === true) n.setAttribute(k, k);
    else if (v !== false && v != null) n.setAttribute(k, v);
  }
  for (const c of children) {
    if (c == null) continue;
    if (typeof c === "string") n.appendChild(document.createTextNode(c));
    else n.appendChild(c);
  }
  return n;
}

const state = {
  bootstrap: null,
  rooms: [],
  queue: [],
  comment: "",
  employeeKey: "dina",
};

function roomPickerTab(roomName) {
  if (!roomName.startsWith("Номер ")) return "other";
  const rest = roomName.replace("Номер ", "").trim();
  if (/^\d+$/.test(rest)) {
    const n = parseInt(rest, 10);
    if (n >= 101 && n <= 109) return "floor1";
    if (n === 403) return "floor4";
    return "other";
  }
  const m = rest.match(/^(\d+)\.(\d+)$/);
  if (!m) return "other";
  const major = parseInt(m[1], 10);
  const minor = parseInt(m[2], 10);
  if (minor < 1 || minor > 4) return "other";
  if (major >= 401 && major <= 402) return "floor4";
  if (major >= 404 && major <= 405) return "floor4";
  return "other";
}

function renderTop(title, sub) {
  return el(
    "div",
    { class: "topbar" },
    el("h1", { class: "title" }, title),
    sub ? el("div", { class: "sub" }, sub) : null
  );
}

function viewSetup() {
  const root = el("div", { class: "wrap stack" });
  root.appendChild(renderTop("Первичная настройка", "PIN + Telegram + MAX + VK"));

  const pin = el("input", { class: "input", type: "password", placeholder: "PIN (мин. 4)" });
  const pin2 = el("input", { class: "input", type: "password", placeholder: "Повтор PIN" });

  const botToken = el("input", { class: "input", placeholder: "BOT_TOKEN" });
  const channelId = el("input", { class: "input", placeholder: "CHANNEL_ID (например -100...)" });
  const channelLink = el("input", { class: "input", placeholder: "CHANNEL_LINK (необязательно)" });

  const maxToken = el("input", { class: "input", placeholder: "MAX_BOT_TOKEN" });
  const maxChatId = el("input", { class: "input", placeholder: "MAX_CHAT_ID" });

  const vkToken = el("input", { class: "input", placeholder: "VK_ACCESS_TOKEN" });
  const vkGroupId = el("input", { class: "input", placeholder: "VK_GROUP_ID (например 12345)" });

  const err = el("div", { class: "err" });

  const saveBtn = el("button", { class: "btn" }, "Сохранить и продолжить");
  saveBtn.onclick = async () => {
    err.textContent = "";
    if (pin.value.trim().length < 4) return (err.textContent = "PIN должен быть не короче 4 символов.");
    if (pin.value.trim() !== pin2.value.trim()) return (err.textContent = "PIN не совпадают.");
    try {
      await api("/api/setup", {
        method: "POST",
        body: {
          pin: pin.value.trim(),
          telegram: {
            botToken: botToken.value.trim(),
            channelId: channelId.value.trim(),
            channelLink: channelLink.value.trim(),
          },
          max: { botToken: maxToken.value.trim(), chatId: maxChatId.value.trim() },
          vk: { accessToken: vkToken.value.trim(), groupId: vkGroupId.value.trim() },
        },
      });
      location.hash = "#/login";
    } catch (e) {
      err.textContent = e.data?.error || e.message;
    }
  };

  root.appendChild(
    el("div", { class: "card stack" },
      el("div", { class: "field" }, el("div", { class: "label" }, "PIN"), pin),
      el("div", { class: "field" }, el("div", { class: "label" }, "Повтор PIN"), pin2),
      el("div", { class: "field" }, el("div", { class: "label" }, "Telegram BOT_TOKEN"), botToken),
      el("div", { class: "field" }, el("div", { class: "label" }, "Telegram CHANNEL_ID"), channelId),
      el("div", { class: "field" }, el("div", { class: "label" }, "Telegram CHANNEL_LINK"), channelLink),
      el("div", { class: "field" }, el("div", { class: "label" }, "MAX_BOT_TOKEN"), maxToken),
      el("div", { class: "field" }, el("div", { class: "label" }, "MAX_CHAT_ID"), maxChatId),
      el("div", { class: "field" }, el("div", { class: "label" }, "VK_ACCESS_TOKEN"), vkToken),
      el("div", { class: "field" }, el("div", { class: "label" }, "VK_GROUP_ID"), vkGroupId),
      err,
      saveBtn
    )
  );
  return root;
}

function viewLogin() {
  const root = el("div", { class: "wrap stack" });
  root.appendChild(renderTop("Вход", "Введите PIN"));
  const pin = el("input", { class: "input", type: "password", placeholder: "PIN" });
  const err = el("div", { class: "err" });
  const btn = el("button", { class: "btn" }, "Войти");
  btn.onclick = async () => {
    err.textContent = "";
    try {
      await api("/api/login", { method: "POST", body: { pin: pin.value } });
      location.hash = "#/menu";
    } catch (e) {
      err.textContent = e.data?.error === "INVALID_PIN" ? "Неверный PIN." : (e.data?.error || e.message);
    }
  };
  const reset = el("button", { class: "btn ghost" }, "Сбросить настройки");
  reset.onclick = async () => {
    // For now: user can delete pwa.db manually; keep simple.
    err.textContent = "Сброс: удалите файл pwa-server/pwa.db на сервере и перезапустите.";
  };
  root.appendChild(el("div", { class: "card stack" }, pin, err, btn, reset));
  return root;
}

function viewMenu() {
  const root = el("div", { class: "wrap stack" });
  root.appendChild(renderTop("Задачи горничных", "Меню"));
  const card = el(
    "div",
    { class: "card stack" },
    el("button", { class: "btn", onclick: () => (location.hash = "#/create") }, "Создать задание"),
    el("button", { class: "btn tonal", onclick: () => (location.hash = "#/history") }, "История"),
    el("button", { class: "btn tonal", onclick: () => (location.hash = "#/rooms") }, "Помещения"),
    el("button", { class: "btn tonal", onclick: () => (location.hash = "#/channel") }, "Ссылка на канал"),
    el("button", { class: "btn ghost", onclick: async () => { await api("/api/logout", {method:"POST"}); location.hash="#/login"; } }, "Выйти")
  );
  root.appendChild(card);
  return root;
}

async function ensureRooms() {
  if (state.rooms.length) return;
  const data = await api("/api/rooms");
  state.rooms = (data.rooms || []).filter((r) => r.isActive);
}

function viewCreateTask() {
  const root = el("div", { class: "wrap stack" });
  root.appendChild(renderTop("Новое задание", "Очередь уборки и отправка"));

  const info = el("div", { class: "card stack" });
  const emp = el("div", {}, `Исполнитель: ${state.employeeKey.toUpperCase()}`);
  const total = el("div", {}, `Площадь: ${Math.round(state.queue.reduce((a, r) => a + (r.area || 0), 0))} м²`);
  info.append(emp, total);

  const queueBox = el("div", { class: "card stack" }, el("div", { class: "h" }, "Очередь"));
  const queueList = el("div", { class: "list" });
  function rerenderQueue() {
    queueList.innerHTML = "";
    if (!state.queue.length) {
      queueList.appendChild(el("div", { class: "help" }, "Пока пусто — добавьте номера/помещения ниже."));
      return;
    }
    state.queue.forEach((it, idx) => {
      const row = el(
        "div",
        { class: "item" },
        el("div", { class: "h" }, `${idx + 1}. ${it.name}`),
        el("div", { class: "m" }, `${Math.round(it.area)} м² · ${it.cleaningType}`),
        el("div", { class: "row" },
          el("button", { class: "btn tonal", onclick: () => { if (idx>0){ const t=state.queue[idx-1]; state.queue[idx-1]=state.queue[idx]; state.queue[idx]=t; rerenderQueue(); } } }, "↑"),
          el("button", { class: "btn tonal", onclick: () => { if (idx<state.queue.length-1){ const t=state.queue[idx+1]; state.queue[idx+1]=state.queue[idx]; state.queue[idx]=t; rerenderQueue(); } } }, "↓"),
          el("button", { class: "btn danger", onclick: () => { state.queue.splice(idx,1); rerenderQueue(); } }, "Удалить")
        )
      );
      queueList.appendChild(row);
    });
  }
  queueBox.appendChild(queueList);
  rerenderQueue();

  const tabState = { tab: "floor1" };
  const tabs = el("div", { class: "tabs" },
    el("button", { class: "tab active", onclick: () => selectTab("floor1") }, "1 этаж · 101–109"),
    el("button", { class: "tab", onclick: () => selectTab("floor4") }, "4 этаж · 401–405"),
    el("button", { class: "tab", onclick: () => selectTab("other") }, "Помещения")
  );
  function selectTab(tab) {
    tabState.tab = tab;
    [...tabs.children].forEach((b) => b.classList.remove("active"));
    const idx = tab === "floor1" ? 0 : tab === "floor4" ? 1 : 2;
    tabs.children[idx].classList.add("active");
    rerenderRooms();
  }

  const roomsBox = el("div", { class: "card stack" }, el("div", { class: "h" }, "Добавить помещение"), tabs);
  const roomsList = el("div", { class: "list" });
  roomsBox.appendChild(roomsList);

  function rerenderRooms() {
    roomsList.innerHTML = "";
    const filtered = state.rooms.filter((r) => roomPickerTab(r.name) === tabState.tab);
    filtered.forEach((r) => {
      const b = el("button", { class: "btn tonal" }, `${r.name} (${r.area.toFixed(2)} м²)`);
      b.onclick = () => openAddFlow(r);
      roomsList.appendChild(b);
    });
  }

  const modal = el("div");
  function openAddFlow(room) {
    // minimal flow: choose cleaning type, then linen variant if needed
    const wrap = el("div", { class: "card stack" });
    wrap.appendChild(el("div", { class: "h" }, room.name));
    wrap.appendChild(el("div", { class: "help" }, "Выберите вид уборки"));
    const types = [
      ["current", "текущая"],
      ["current_linen", "текущая/смена белья"],
      ["departure", "выезд"],
      ["departure_arrival", "выезд/заезд"],
      ["general", "генеральная"],
    ];
    types.forEach(([k, label]) => {
      const b = el("button", { class: "btn tonal" }, label);
      b.onclick = () => {
        const item = { id: room.id, name: room.name, area: room.area, cleaningType: k };
        state.queue.push(item);
        modal.innerHTML = "";
        rerenderQueue();
      };
      wrap.appendChild(b);
    });
    wrap.appendChild(el("button", { class: "btn ghost", onclick: () => (modal.innerHTML = "") }, "Отмена"));
    modal.innerHTML = "";
    modal.appendChild(wrap);
  }

  const comment = el("textarea", { class: "input", rows: "3", placeholder: "Комментарий (необязательно)" });
  comment.value = state.comment || "";
  comment.oninput = () => (state.comment = comment.value);

  const err = el("div", { class: "err" });

  const sendRow = el("div", { class: "card stack" },
    el("div", { class: "h" }, "Отправка"),
    el("div", { class: "row" },
      el("button", { class: "btn", onclick: () => doSend(["telegram"]) }, "Telegram"),
      el("button", { class: "btn tonal", onclick: () => doSend(["max"]) }, "MAX"),
      el("button", { class: "btn tonal", onclick: () => doSend(["vk"]) }, "VK")
    ),
    el("div", { class: "row" },
      el("button", { class: "btn ghost", onclick: () => { state.queue = []; rerenderQueue(); } }, "Очистить"),
      el("button", { class: "btn ghost", onclick: () => (location.hash = "#/menu") }, "В меню")
    ),
    el("div", { class: "field" }, el("div", { class: "label" }, "Комментарий"), comment),
    err
  );

  async function doSend(sendTo) {
    err.textContent = "";
    try {
      await api("/api/tasks/send", {
        method: "POST",
        body: {
          employeeKey: state.employeeKey,
          comment: state.comment,
          queue: state.queue,
          sendTo,
        },
      });
      state.queue = [];
      state.comment = "";
      location.hash = "#/history";
    } catch (e) {
      err.textContent = e.data?.message || e.data?.error || e.message;
    }
  }

  root.append(info, queueBox, roomsBox, modal, sendRow);
  return root;
}

async function viewHistory() {
  const root = el("div", { class: "wrap stack" });
  root.appendChild(renderTop("История", "Последние 50 заданий"));
  const back = el("button", { class: "btn ghost", onclick: () => (location.hash = "#/menu") }, "В меню");
  const box = el("div", { class: "card stack" });
  const list = el("div", { class: "list" });
  box.appendChild(list);
  try {
    const data = await api("/api/tasks");
    (data.tasks || []).forEach((t) => {
      const it = el("div", { class: "item" },
        el("div", { class: "h" }, `#${t.id} · ${t.employeeKey}`),
        el("div", { class: "m" }, `${new Date(t.createdAt).toLocaleString()} · ${Math.round(t.totalArea)} м²`),
        el("button", { class: "btn tonal", onclick: () => (location.hash = `#/task/${t.id}`) }, "Открыть")
      );
      list.appendChild(it);
    });
    if (!(data.tasks || []).length) list.appendChild(el("div", { class: "help" }, "Пока нет заданий."));
  } catch (e) {
    list.appendChild(el("div", { class: "err" }, e.data?.error || e.message));
  }
  root.append(box, back);
  return root;
}

async function viewTaskDetail(id) {
  const root = el("div", { class: "wrap stack" });
  root.appendChild(renderTop(`Задание #${id}`, "Детали"));
  const box = el("div", { class: "card stack" });
  const back = el("button", { class: "btn ghost", onclick: () => (location.hash = "#/history") }, "Назад");
  try {
    const data = await api(`/api/tasks/${id}`);
    const t = data.task;
    box.appendChild(el("div", {}, `Исполнитель: ${t.employeeKey}`));
    box.appendChild(el("div", {}, `Время: ${new Date(t.createdAt).toLocaleString()}`));
    box.appendChild(el("div", {}, `Площадь: ${Math.round(t.totalArea)} м²`));
    if (t.comment) box.appendChild(el("div", { class: "mono" }, `Комментарий: ${t.comment}`));
    box.appendChild(el("div", { class: "mono" }, JSON.stringify(t.rooms, null, 2)));
  } catch (e) {
    box.appendChild(el("div", { class: "err" }, e.data?.error || e.message));
  }
  root.append(box, back);
  return root;
}

async function viewRooms() {
  const root = el("div", { class: "wrap stack" });
  root.appendChild(renderTop("Помещения", "Управление"));
  const err = el("div", { class: "err" });
  const listBox = el("div", { class: "card stack" });
  const list = el("div", { class: "list" });
  listBox.appendChild(list);
  const back = el("button", { class: "btn ghost", onclick: () => (location.hash = "#/menu") }, "В меню");

  async function reload() {
    list.innerHTML = "";
    err.textContent = "";
    try {
      const data = await api("/api/rooms");
      const rooms = data.rooms || [];
      rooms.forEach((r) => {
        const row = el("div", { class: "item" },
          el("div", { class: "h" }, r.name),
          el("div", { class: "m" }, `${r.area.toFixed(2)} м² · ${r.isActive ? "активно" : "выкл."}`),
          el("div", { class: "row" },
            el("button", { class: "btn tonal", onclick: async () => { await api(`/api/rooms/${r.id}/toggle`, {method:"POST"}); await reload(); } }, r.isActive ? "Откл" : "Вкл"),
            el("button", { class: "btn ghost", onclick: async () => {
              const a = prompt("Новая площадь:", String(r.area));
              if (!a) return;
              await api(`/api/rooms/${r.id}/area`, {method:"POST", body:{area: a}});
              await reload();
            } }, "Площадь")
          )
        );
        list.appendChild(row);
      });
    } catch (e) {
      err.textContent = e.data?.error || e.message;
    }
  }

  const addBtn = el("button", { class: "btn" }, "Добавить помещение");
  addBtn.onclick = async () => {
    const name = prompt("Название помещения:");
    if (!name) return;
    const area = prompt("Площадь (число):");
    if (!area) return;
    try {
      await api("/api/rooms", { method: "POST", body: { name, area } });
      await reload();
    } catch (e) {
      err.textContent = e.data?.error || e.message;
    }
  };

  root.append(el("div", { class: "card stack" }, addBtn, err), listBox, back);
  await reload();
  return root;
}

async function viewChannel() {
  const root = el("div", { class: "wrap stack" });
  root.appendChild(renderTop("Ссылка на канал", ""));
  const box = el("div", { class: "card stack" });
  const back = el("button", { class: "btn ghost", onclick: () => (location.hash = "#/menu") }, "В меню");
  const data = await api("/api/bootstrap").catch(() => ({ channelLink: null }));
  box.appendChild(el("div", { class: "mono" }, data.channelLink || "(не задана)"));
  root.append(box, back);
  return root;
}

async function render() {
  const app = document.getElementById("app");
  const hash = location.hash || "#/";

  if (!state.bootstrap) {
    state.bootstrap = await api("/api/bootstrap").catch(() => ({ setupComplete: false }));
  }

  let view = null;
  try {
    if (!state.bootstrap.setupComplete && hash !== "#/setup") {
      location.hash = "#/setup";
      return;
    }
    if (hash === "#/setup") view = viewSetup();
    else if (hash === "#/login") view = viewLogin();
    else if (hash === "#/menu") view = viewMenu();
    else if (hash === "#/create") {
      await ensureRooms();
      view = viewCreateTask();
    } else if (hash === "#/history") view = await viewHistory();
    else if (hash.startsWith("#/task/")) view = await viewTaskDetail(parseInt(hash.split("/")[2], 10));
    else if (hash === "#/rooms") view = await viewRooms();
    else if (hash === "#/channel") view = await viewChannel();
    else view = viewMenu();
  } catch (e) {
    view = el("div", { class: "wrap stack" }, el("div", { class: "err" }, e.message));
  }

  app.innerHTML = "";
  app.appendChild(view);
}

window.addEventListener("hashchange", render);
render();

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("/sw.js").catch(() => {});
  });
}

