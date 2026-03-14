// This file can be replaced during build by using the `fileReplacements` array.
// `ng build` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
    production: false,
    springboot: "http://localhost:8080",
    minio: "http://localhost:9000",
    // springboot: "",
    // minio: "",

    firebase: {
        apiKey: "AIzaSyAxDhw4uOmEoq-Yew4G-Zbe6K-5GDMzsCE",
        authDomain: "lumos-push.firebaseapp.com",
        projectId: "lumos-push",
        storageBucket: "lumos-push.firebasestorage.app",
        messagingSenderId: "37243759038",
        appId: "1:37243759038:web:47343c71f1c322ef7a31ef",
        measurementId: "G-WVZZ5PRZKY"
    }
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/plugins/zone-error';  // Included with Angular CLI.
