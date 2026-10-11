#![no_main]
use libfuzzer_sys::fuzz_target;
use warifu_core::uri::parse_otpauth_uri;

fuzz_target!(|data: &[u8]| {
    if let Ok(s) = std::str::from_utf8(data) {
        let _ = parse_otpauth_uri(s);
    }
});
