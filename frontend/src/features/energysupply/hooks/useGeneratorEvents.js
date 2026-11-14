import { useCallback, useEffect, useState } from "react";
import {
  createGeneratorEvent,
  deleteGeneratorEvent,
  getDashboardSummary,
  getGeneratorEvents,
  updateGeneratorEvent,
} from "../api/energyApi";

function messageFrom(error) {
  return error?.message || "An unexpected error occurred.";
}

export function useGeneratorEvents() {
  const [events, setEvents] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [nextEvents, nextSummary] = await Promise.all([
        getGeneratorEvents(),
        getDashboardSummary(),
      ]);
      setEvents(nextEvents);
      setSummary(nextSummary);
    } catch (requestError) {
      setError(messageFrom(requestError));
      throw requestError;
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    Promise.all([getGeneratorEvents(), getDashboardSummary()])
      .then(([nextEvents, nextSummary]) => {
        setEvents(nextEvents);
        setSummary(nextSummary);
      })
      .catch((requestError) => setError(messageFrom(requestError)))
      .finally(() => setLoading(false));
  }, []);

  const saveEvent = useCallback(async (eventId, payload) => {
    setSaving(true);
    setError(null);
    try {
      if (eventId) {
        await updateGeneratorEvent(eventId, payload);
      } else {
        await createGeneratorEvent(payload);
      }
      await refresh();
    } catch (requestError) {
      setError(messageFrom(requestError));
      throw requestError;
    } finally {
      setSaving(false);
    }
  }, [refresh]);

  const removeEvent = useCallback(async (eventId) => {
    setSaving(true);
    setError(null);
    try {
      await deleteGeneratorEvent(eventId);
      await refresh();
    } catch (requestError) {
      setError(messageFrom(requestError));
      throw requestError;
    } finally {
      setSaving(false);
    }
  }, [refresh]);

  return { events, summary, loading, saving, error, refresh, saveEvent, removeEvent };
}
