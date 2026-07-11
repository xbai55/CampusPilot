function parseMaybeJson(value) {
  if (typeof value !== "string") return value;
  const text = value.trim();
  if (!text) return value;
  if (!["{", "["].includes(text[0])) return value;
  try {
    return JSON.parse(text);
  } catch (err) {
    return value;
  }
}

function firstDefined(...values) {
  return values.find((item) => item !== undefined && item !== null && item !== "");
}

function listToConfigMap(configItems) {
  if (!Array.isArray(configItems)) return {};
  return configItems.reduce((acc, item) => {
    const key = item?.key || item?.name || item?.id;
    if (key) acc[key] = firstDefined(item.value, item.defaultValue, item.text);
    return acc;
  }, {});
}

export function normalizeKData(input = {}) {
  const props = parseMaybeJson(input) || {};
  const data = parseMaybeJson(firstDefined(props.data, props.value, props.result, props.rows));
  const configItems = parseMaybeJson(props.configItems) || [];
  const configMap = listToConfigMap(configItems);
  const cardRowData = parseMaybeJson(firstDefined(props.cardRowData, props.cardData, props.rowData));

  return {
    raw: props,
    data: data && typeof data === "object" ? data : null,
    configItems: Array.isArray(configItems) ? configItems : [],
    cardRowData: cardRowData || null,
    themeColor: firstDefined(props.themeColor, props.theme, configMap.themeColor, "#2f6bff"),
    lock: Boolean(firstDefined(props.lock, props.readonly, configMap.lock, false)),
  };
}

export default normalizeKData;
