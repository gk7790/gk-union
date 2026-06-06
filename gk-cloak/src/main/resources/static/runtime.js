(function (window) {

    const FPSDK = {
        version: "2.0.0",

        async init(options = {}) {
            this.options = {
                debug: false,
                ...options
            };
            return this;
        },

        // ================= 主入口 =================
        async collect() {
            const baseInfo = this.getBaseInfo();
            const browserInfo = this.getBrowserInfo();
            const hardwareInfo = this.getHardwareInfo();
            const behaviorInfo = this.getBehaviorInfo();
            const entropyInfo = await this.getEntropyInfo();

            const raw = {
                version: this.version,
                baseInfo,
                browserInfo,
                hardwareInfo,
                behaviorInfo,
                entropyInfo,
                timestamp: Date.now()
            };

            const fingerprint = await this.hash(JSON.stringify(raw));

            const result = {
                ...raw,
                fingerprint
            };

            if (this.options.debug) {
                console.log("[FPSDK]", result);
            }

            return result;
        },

        // ================= 稳定信息 =================
        getBaseInfo() {
            return {
                timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
                language: navigator.language,
                platform: navigator.platform
            };
        },

        // ================= 浏览器信息 =================
        getBrowserInfo() {
            return {
                ua: navigator.userAgent,
                plugins: navigator.plugins?.length || 0,
                webdriver: !!navigator.webdriver
            };
        },

        // ================= 硬件信息 =================
        getHardwareInfo() {
            return {
                cpu: navigator.hardwareConcurrency || 0,
                memory: navigator.deviceMemory || 0,
                touch: navigator.maxTouchPoints || 0,
                screen: {
                    w: screen.width,
                    h: screen.height,
                    dpr: window.devicePixelRatio || 1
                }
            };
        },

        // ================= 行为/环境信号 =================
        getBehaviorInfo() {
            return {
                online: navigator.onLine,
                cookieEnabled: navigator.cookieEnabled
            };
        },

        // ================= 高熵指纹（重点优化） =================
        async getEntropyInfo() {
            return {
                canvas: this.getCanvasFingerprint(),
                webgl: this.getWebGLFingerprint()
            };
        },

        // ================= Canvas 指纹（降噪版） =================
        getCanvasFingerprint() {
            try {
                const canvas = document.createElement("canvas");
                const ctx = canvas.getContext("2d");

                ctx.textBaseline = "top";
                ctx.font = "14px Arial";
                ctx.fillStyle = "#f60";
                ctx.fillRect(10, 10, 100, 30);
                ctx.fillStyle = "#069";
                ctx.fillText("fp-sdk-v2", 2, 15);

                const data = canvas.toDataURL();

                // ⚠️ 只取 hash 前处理（避免长字符串污染 fingerprint）
                return this.simpleHash(data);
            } catch {
                return null;
            }
        },

        // ================= WebGL 指纹（安全版） =================
        getWebGLFingerprint() {
            try {
                const canvas = document.createElement("canvas");
                const gl = canvas.getContext("webgl");

                if (!gl) return null;

                const ext = gl.getExtension("WEBGL_debug_renderer_info");

                const vendor = ext
                    ? gl.getParameter(ext.UNMASKED_VENDOR_WEBGL)
                    : "unknown";

                const renderer = ext
                    ? gl.getParameter(ext.UNMASKED_RENDERER_WEBGL)
                    : "unknown";

                return this.simpleHash(vendor + "|" + renderer);
            } catch {
                return null;
            }
        },

        // ================= hash（主 fingerprint） =================
        async hash(str) {
            const buffer = new TextEncoder().encode(str);
            const hash = await crypto.subtle.digest("SHA-256", buffer);

            return Array.from(new Uint8Array(hash))
                .map(b => b.toString(16).padStart(2, "0"))
                .join("");
        },

        // ================= 快速 hash（用于子模块） =================
        simpleHash(str) {
            let h = 0;
            for (let i = 0; i < str.length; i++) {
                h = (h << 5) - h + str.charCodeAt(i);
                h |= 0;
            }
            return h.toString(16);
        }
    };

    FPSDK.init().then(async () => {
        const data = await FPSDK.collect();
        console.log("fingerprint:", data);
    });
    
    window.FPSDK = FPSDK;

})(window);