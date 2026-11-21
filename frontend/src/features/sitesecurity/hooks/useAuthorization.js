import { useEffect, useState } from "react";
import { fetchAuthorization } from "../api/sitesecurityApi";

/*
 * The caller's Site Security authorization (role, permissions, zones), resolved by the server
 * from the platform identity. Site Security has no sign-in of its own: without a platform
 * identity the server answers 401 and the module stays closed.
 */
export function useAuthorization() {
  const [authorization, setAuthorization] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorStatus, setErrorStatus] = useState(null);

  useEffect(() => {
    let isMounted = true;
    fetchAuthorization()
      .then((result) => isMounted && setAuthorization(result))
      .catch((err) => isMounted && setErrorStatus(err.status ?? 0))
      .finally(() => isMounted && setIsLoading(false));
    return () => {
      isMounted = false;
    };
  }, []);

  return { authorization, isLoading, errorStatus };
}
