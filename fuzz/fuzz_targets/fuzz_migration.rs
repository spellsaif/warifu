#![no_main]
use libfuzzer_sys::fuzz_target;
use warifu_core::google_migration::parse_google_migration_uri;

fuzz_target!(|data: &[u8]| {
    if let Ok(s) = std::str::from_utf8(data) {
        let _ = parse_google_migration_uri(s);
    }
});
