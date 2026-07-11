
import { createElement } from '@kdcloudjs/kwc';

// 杩炴帴绗?
const CONNECTOR = '__$$__';
const ctxMap = new Map();

const getInstanceId = (m) => `${m.pageId}${CONNECTOR}${m.key}`;

(function (KDApi) {
    function MyComponent (model) {
        this._initInternalState(model);
    }

    MyComponent.prototype = {
        _initInternalState: function (model) {
            this.model = model;
            this.instanceId = getInstanceId(model);
            this._isDestroyed = false; // 寮傛鍔犺浇瀹屾垚鍓嶅氨鍙兘琚攢姣?
        },
        init: function (props) {
            const ctx = { model: this.model, props };
            ctxMap.set(this.instanceId, ctx);
            const { dom } = this.model;
            // 寮傛鍔犺浇
            import('campuspilot/app').then(({ default: App }) => {
                if (this._isDestroyed) { return; }
                const elm = createElement('campuspilot-app', { is: App });
                elm.instanceId = this.instanceId;
                dom.appendChild(elm);
            });
        },
        update: function (props) {
            const ctx = ctxMap.get(this.instanceId);
            if (ctx) { ctx.props = props; }
        },
        handleDirective: function () {
            // 鍙傛暟锛歝ustomProps, methodname, args
            // 杩欓噷鐨刴ethodname 瀵瑰簲鐨勬槸鎸囦护鍙戣繃鏉ュ畾涔夌殑methodname锛屽彲鏍规嵁鏂规硶鍚嶆嬁鍒板搴旂殑鍙傛暟args
        },
        destoryed: function () {
            this._isDestroyed = true;
            ctxMap.delete(this.instanceId);
        }
        // 浠ヤ笅鐢熷懡鍛ㄦ湡鍦╒7.0.4+鐗堟湰鏀寔
        // 鏂扮増鐢熷懡鍛ㄦ湡鏃犳硶涓庢棫鐗堟湰update鍚屾椂浣跨敤

        // onPropsUpdate: function (this: ComponentInstance, props: TCustomProps) {
        //   // 浠绘剰props鏁版嵁鍙樻洿鏃惰Е鍙?
        //   console.log('-----onPropsUpdate', this.model, props)
        // },

        // onThemeUpdate: function (this: ComponentInstance, props: IThemeUpdateProps) {
        //   // 涓婚鍙樻洿鏃惰Е鍙?
        //   console.log('-----onThemeUpdate', this.model, props)
        // },

        // onDataUpdate: function (this: ComponentInstance, props: IDataUpdateProps) {
        //   // 鎺т欢鏁版嵁鍙樻洿鏃惰Е鍙?
        //   console.log('-----onDataUpdate', this.model, props)
        // },

        // onLockUpdate: function (this: ComponentInstance, props: ILockUpdateProps) {
        //   // 鎺т欢閿佸畾鎬у彉鏇存椂瑙﹀彂
        //   console.log('-----onLockUpdate', this.model, props)
        // },

        // onCardRowDataUpdate: function (this: ComponentInstance, props: ICardRowDataUpdateProps) {
        //   // 鍗＄墖琛屾暟鎹彉鏇存椂瑙﹀彂
        //   console.log('-----onCardRowDataUpdate', this.model, props)
        // },

        // onGridRowDataUpdate: function (this: ComponentInstance, props: IGridRowDataUpdateProps) {
        //   // 鍗曟嵁浣撹鏁版嵁鍙樻洿鏃惰Е鍙?
        //   console.log('-----onGridRowDataUpdate', this.model, props)
        // },
    };

    // 娉ㄥ唽鑷畾涔夌粍浠?
    KDApi.register('campuspilot', MyComponent, {
        isMulLang: false
    });
})(window.KDApi);

/**
 * 鑾峰彇缁勪欢涓婁笅鏂囦俊鎭?
 * @param {string} instanceId  pageId__$$__componentId
 * @returns {object|null} 杩斿洖缁勪欢涓婁笅鏂囷紝鍖呭惈 model 鍜?props 灞炴€э紱濡傛灉鏈壘鍒板搴斾笂涓嬫枃锛屽垯杩斿洖 null
 */
export function getComponentContext(instanceId) {
    return ctxMap.get(instanceId) || null;
}
