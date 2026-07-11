'use strict'

const { defineConfig } = require('eslint/config')
const kingdeeKwcConfig = require('@kdcloudjs/eslint-config-kwc/recommended')

module.exports = defineConfig([
  {
    files: ['src/**/*.js'],
    extends: [kingdeeKwcConfig],
    languageOptions: {
      globals: {
        require: 'readonly'
      }
    },
    rules: {
      'jest/no-deprecated-functions': 'off',
      'semi': ['error', 'always'], // 寮哄埗浣跨敤鍒嗗彿
      'indent': [
          2,
          4,
          { SwitchCase: 1 } // switch 璇彞涓殑 case 鍒嗘敮浣跨敤 1 涓┖鏍肩缉杩?
      ],
      'no-multi-spaces': 2, // 涓嶅厑璁稿涓繛缁殑绌烘牸
      'space-unary-ops': [2, { words: true, nonwords: false }], // 涓€鍏冭繍绠楃鍚庡繀椤绘湁绌烘牸
      'space-before-blocks': [2, 'always'], // 浠ｇ爜鍧楀墠蹇呴』鏈夌┖鏍?
      'no-mixed-spaces-and-tabs': 2, // 涓嶅厑璁告贩鍚堜娇鐢ㄧ┖鏍煎拰鍒惰〃绗?
      'no-multiple-empty-lines': [2, { max: 1 }], // 杩炵画绌鸿涓嶈秴杩?1 琛?
      'no-trailing-spaces': 2, // 琛屽熬涓嶅厑璁告湁绌烘牸
      'no-whitespace-before-property': 2, // 灞炴€у悕鍜岀偣杩愮畻绗︿箣闂翠笉鑳芥湁绌烘牸
      'no-irregular-whitespace': 2, // 涓嶅厑璁稿嚭鐜颁笉瑙勫垯鐨勭┖鐧藉瓧绗?
      'space-in-parens': [1, 'never'], // 鍦嗘嫭鍙峰唴涓嶈兘鏈夌┖鏍?
      'comma-dangle': [1, 'never'], // 閫楀彿涓嶅厑璁告湁鎷栧熬
      'max-len': ['error', { code: 200 }], // 琛屽鏈€澶т负 200 瀛楃
      'operator-linebreak': [2, 'before'], // 杩愮畻绗︽崲琛屾椂锛岃繍绠楃鍦ㄨ棣?
      'comma-style': [2, 'last'], // 閫楀彿椋庢牸锛氭崲琛屾椂鍦ㄨ灏?
      'no-extra-semi': 2, // 涓嶅厑璁稿嚭鐜板浣欑殑鍒嗗彿
      'curly': [2, 'all'], // 浣跨敤澶ф嫭鍙峰寘瑁规墍鏈夋帶鍒剁粨鏋?
      'key-spacing': [2, { beforeColon: false, afterColon: true }], // 灞炴€у悕涓庡啋鍙蜂箣闂翠笉鑳芥湁绌烘牸锛屽啋鍙峰悗蹇呴』鏈夌┖鏍?
      'comma-spacing': [1, { before: false, after: true }], // 閫楀彿鍚庡繀椤绘湁绌烘牸
      'spaced-comment': [1, 'always'], // 娉ㄩ噴鍚庡繀椤绘湁绌烘牸
      'eqeqeq': [2, 'always', { null: 'ignore' }], // 寮哄埗浣跨敤鍏ㄧ瓑 (===) 杩愮畻绗?
      'no-else-return': [1, { allowElseIf: false }], // 绂佹 else 璇彞锛屽鏋?if 璇彞涓凡杩斿洖鍊?
      'no-loop-func': 2, // 绂佹鍦ㄥ惊鐜腑瀹氫箟鍑芥暟
      'no-implicit-coercion': [1, { allow: ['!!'] }], // 绂佹闅愬紡绫诲瀷杞崲
      'quotes': [2, 'single'], // 寮哄埗浣跨敤鍗曞紩鍙?
      'max-params': [1, 6], // 鍑芥暟鍙傛暟鏈€澶ф暟閲忎负 6
      'no-eval': 2, // 绂佹浣跨敤 eval
      'prefer-const': 2, // 寤鸿浣跨敤 const 澹版槑涓嶅彉鐨勫彉閲?
      'no-var': 2, // 寤鸿浣跨敤 let/const 鏇夸唬 var
      'prefer-destructuring': [
          1,
          { object: true, array: false } // 寤鸿浣跨敤瑙ｆ瀯璧嬪€?
      ],
      'prefer-template': 1, // 寤鸿浣跨敤妯℃澘瀛楃涓?
      'no-duplicate-imports': 2, // 绂佹閲嶅瀵煎叆
      'no-unused-vars': 1, // 鍑虹幇鏈娇鐢ㄧ殑鍙橀噺
      'radix': 1 // 瑙ｆ瀽鏁存暟鏃跺繀椤讳娇鐢ㄥ熀鏁?
    }
  }
])