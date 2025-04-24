#ifndef __UTIL_H__
#define __UTIL_H__

#include <string>
#include <sstream>
#include <vector>

// The following 3 functions were adapted from
// http://stackoverflow.com/questions/479080/trim-is-not-part-of-the-standard-c-c-library?rq=1
static const std::string whiteSpaces( " \f\n\r\t\v" );

// Remove initial and trailing whitespace from string. Modifies string in-place
inline void trimRight( std::string& str,
                       const std::string& trimChars = whiteSpaces )
{
   std::string::size_type pos = str.find_last_not_of( trimChars );
   str.erase( pos + 1 );
}

inline void trimLeft( std::string& str,
                      const std::string& trimChars = whiteSpaces )
{
   std::string::size_type pos = str.find_first_not_of( trimChars );
   str.erase( 0, pos );
}

inline void trim( std::string& str,
                  const std::string& trimChars = whiteSpaces )
{
   trimRight( str, trimChars );
   trimLeft( str, trimChars );
}


inline bool endsWith(const std::string& fullString, const std::string& ending)
{
   if (fullString.length() >= ending.length())
   {
      return (0 == fullString.compare (fullString.length() - ending.length(), ending.length(), ending));
   }
   else
   {
      return false;
   }
}


// The following 2 functions were adapted from http://stackoverflow.com/questions/236129/how-to-split-a-string-in-c
inline void
split(const std::string &s, char delim, std::vector<std::string> &elems)
{
    std::stringstream ss(s);
    std::string item;
    while (std::getline(ss, item, delim))
    {
        if (item.length() > 0)
            elems.push_back(item);
    }
}


inline std::vector<std::string>
split(const std::string &s, char delim = ' ')
{
    std::vector<std::string> elems;
    split(s, delim, elems);
    return elems;
}

#endif
