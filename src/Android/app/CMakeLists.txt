cmake_minimum_required(VERSION 3.4.1)

project("mdnsd-appleTV")

#add_subdirectory(src/main/cpp/lib/fdk-aac)
add_subdirectory(src/main/cpp/mDNSResponder)


target_link_libraries(jdns_sd)

#target_link_libraries(fdk-aac)