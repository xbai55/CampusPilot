
import { createElement } from '@kdcloudjs/kwc';

(async () => {
    const { default: App } = await import('campuspilot/app');
    const elm = createElement('campuspilot-app', { is: App });
    document.body.appendChild(elm);
})();